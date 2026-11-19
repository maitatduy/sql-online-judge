import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    LessonRequest,
    StudioChapter,
    StudioCourse,
    StudioLessonOutline,
} from "@/features/course-studio/types";

import { apiError } from "./api-error";
import { readMockSession } from "./session";
import { findOwnedCourse, updateCourseTotals } from "./studio-courses";

const STORAGE_KEY = "sqljudge:mock:studio-course-outline";

type OutlineStore = Record<string, StudioChapter[]>;

function lesson(id: string, slug: string, title: string, minutes: number): StudioLessonOutline {
    return { id, slug, title, estimatedMinutes: minutes };
}

function seededOutline(): OutlineStore {
    return {
        "0192f3f1-0000-7000-8000-000000000001": [
            {
                id: "0192f3f2-0000-7000-8000-000000000001",
                title: "Xếp hạng",
                position: 1,
                lessons: [
                    lesson(
                        "0192f3f3-0000-7000-8000-000000000001",
                        "row-number-va-rank",
                        "ROW_NUMBER và RANK",
                        20,
                    ),
                    lesson("0192f3f3-0000-7000-8000-000000000002", "dense-rank", "DENSE_RANK", 20),
                ],
            },
            {
                id: "0192f3f2-0000-7000-8000-000000000002",
                title: "Cộng dồn",
                position: 2,
                lessons: [
                    lesson("0192f3f3-0000-7000-8000-000000000003", "sum-over", "SUM với OVER", 25),
                    lesson(
                        "0192f3f3-0000-7000-8000-000000000004",
                        "khung-cua-so",
                        "Khung cửa sổ",
                        25,
                    ),
                ],
            },
        ],
        "0192f3f1-0000-7000-8000-000000000002": [
            {
                id: "0192f3f2-0000-7000-8000-000000000003",
                title: "Ghép hai bảng",
                position: 1,
                lessons: [
                    lesson("0192f3f3-0000-7000-8000-000000000005", "inner-join", "INNER JOIN", 20),
                    lesson("0192f3f3-0000-7000-8000-000000000006", "left-join", "LEFT JOIN", 20),
                    lesson("0192f3f3-0000-7000-8000-000000000007", "self-join", "Tự ghép bảng", 20),
                ],
            },
        ],
    };
}

function readOutline(): OutlineStore {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as OutlineStore) : seededOutline();
}

function writeOutline(courseId: string, chapters: StudioChapter[]): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ ...readOutline(), [courseId]: chapters }));
    const lessons = chapters.flatMap((chapter) => chapter.lessons);
    updateCourseTotals(
        courseId,
        lessons.length,
        lessons.reduce((total, item) => total + item.estimatedMinutes, 0),
    );
}

type Located = { course: StudioCourse; chapters: StudioChapter[] };

function locate(predicate: (chapters: StudioChapter[]) => boolean): Located | Response {
    const user = readMockSession();
    if (!user) {
        return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
    }
    for (const [courseId, chapters] of Object.entries(readOutline())) {
        const course = findOwnedCourse(courseId, user.id);
        if (course && predicate(chapters)) {
            return { course, chapters };
        }
    }
    return apiError(404, "COURSE_NOT_FOUND", "Không tìm thấy khoá học");
}

function editable(located: Located | Response): Located | Response {
    if (located instanceof Response || located.course.revisionStatus === "DRAFT") {
        return located;
    }
    return apiError(422, "COURSE_LOCKED", "Khoá đang chờ duyệt nên chưa sửa được");
}

function byCourse(courseId: string) {
    const user = readMockSession();
    const course = user ? findOwnedCourse(courseId, user.id) : undefined;
    if (!course) {
        return user
            ? apiError(404, "COURSE_NOT_FOUND", "Không tìm thấy khoá học")
            : apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
    }
    return { course, chapters: readOutline()[courseId] ?? [] };
}

const hasChapter = (chapterId: string) => (chapters: StudioChapter[]) =>
    chapters.some((chapter) => chapter.id === chapterId);

const hasLesson = (lessonId: string) => (chapters: StudioChapter[]) =>
    chapters.some((chapter) => chapter.lessons.some((item) => item.id === lessonId));

export const studioCourseOutlineHandlers: RequestHandler[] = [
    http.get("*/api/studio/courses/:courseId/chapters", async ({ params }) => {
        await delay();
        const located = byCourse(String(params.courseId));
        return located instanceof Response ? located : HttpResponse.json(located.chapters);
    }),
    http.post("*/api/studio/courses/:courseId/chapters", async ({ params, request }) => {
        await delay();
        const located = editable(byCourse(String(params.courseId)));
        if (located instanceof Response) {
            return located;
        }
        const { title } = (await request.json()) as { title: string };
        const chapter: StudioChapter = {
            id: crypto.randomUUID(),
            title,
            position: located.chapters.length + 1,
            lessons: [],
        };
        writeOutline(located.course.id, [...located.chapters, chapter]);
        return HttpResponse.json(chapter, { status: 201 });
    }),
    http.patch("*/api/studio/chapters/:chapterId", async ({ params, request }) => {
        await delay();
        const chapterId = String(params.chapterId);
        const located = editable(locate(hasChapter(chapterId)));
        if (located instanceof Response) {
            return located;
        }
        const { title } = (await request.json()) as { title: string };
        const chapters = located.chapters.map((chapter) =>
            chapter.id === chapterId ? { ...chapter, title } : chapter,
        );
        writeOutline(located.course.id, chapters);
        return HttpResponse.json(chapters.find((chapter) => chapter.id === chapterId));
    }),
    http.delete("*/api/studio/chapters/:chapterId", async ({ params }) => {
        await delay();
        const chapterId = String(params.chapterId);
        const located = editable(locate(hasChapter(chapterId)));
        if (located instanceof Response) {
            return located;
        }
        if (
            located.chapters.some((chapter) => chapter.id === chapterId && chapter.lessons.length)
        ) {
            return apiError(
                422,
                "CHAPTER_NOT_EMPTY",
                "Chuyển hoặc xoá bài giảng trước khi xoá chương",
            );
        }
        writeOutline(
            located.course.id,
            located.chapters.filter((chapter) => chapter.id !== chapterId),
        );
        return new HttpResponse(null, { status: 204 });
    }),
    http.post("*/api/studio/chapters/:chapterId/lessons", async ({ params, request }) => {
        await delay();
        const chapterId = String(params.chapterId);
        const located = editable(locate(hasChapter(chapterId)));
        if (located instanceof Response) {
            return located;
        }
        const body = (await request.json()) as LessonRequest;
        if (
            located.chapters.some((chapter) =>
                chapter.lessons.some((item) => item.slug === body.slug),
            )
        ) {
            return apiError(409, "SLUG_TAKEN", "Slug đã được bài giảng khác trong khoá dùng");
        }
        const created = lesson(crypto.randomUUID(), body.slug, body.title, body.estimatedMinutes);
        const chapters = located.chapters.map((chapter) =>
            chapter.id === chapterId
                ? { ...chapter, lessons: [...chapter.lessons, created] }
                : chapter,
        );
        writeOutline(located.course.id, chapters);
        const position = chapters.find((chapter) => chapter.id === chapterId)?.lessons.length ?? 1;
        return HttpResponse.json(
            { ...created, chapterId, body: body.body, position },
            { status: 201 },
        );
    }),
    http.delete("*/api/studio/lessons/:lessonId", async ({ params }) => {
        await delay();
        const lessonId = String(params.lessonId);
        const located = editable(locate(hasLesson(lessonId)));
        if (located instanceof Response) {
            return located;
        }
        writeOutline(
            located.course.id,
            located.chapters.map((chapter) => ({
                ...chapter,
                lessons: chapter.lessons.filter((item) => item.id !== lessonId),
            })),
        );
        return new HttpResponse(null, { status: 204 });
    }),
];

const BODIES_KEY = "sqljudge:mock:studio-lesson-bodies";
const SEEDED_BODIES: Record<string, string> = {
    "0192f3f3-0000-7000-8000-000000000001":
        "## Đánh số thứ tự\n\n`ROW_NUMBER()` đánh số từng dòng trong mỗi nhóm.",
};

function readBodies(): Record<string, string> {
    const stored = sessionStorage.getItem(BODIES_KEY);
    return stored ? (JSON.parse(stored) as Record<string, string>) : SEEDED_BODIES;
}

function lessonResponse(chapters: StudioChapter[], lessonId: string) {
    const chapter = chapters.find((item) => item.lessons.some((found) => found.id === lessonId));
    const index = chapter?.lessons.findIndex((found) => found.id === lessonId) ?? -1;
    const outline = chapter?.lessons[index];
    return outline && chapter
        ? {
              ...outline,
              chapterId: chapter.id,
              position: index + 1,
              body: readBodies()[lessonId] ?? "",
          }
        : undefined;
}

function lessonNotFound(located: Response): Response {
    return located.status === 404
        ? apiError(404, "LESSON_NOT_FOUND", "Không tìm thấy bài giảng")
        : located;
}

export const studioLessonHandlers: RequestHandler[] = [
    http.get("*/api/studio/lessons/:lessonId", async ({ params }) => {
        await delay();
        const lessonId = String(params.lessonId);
        const located = locate(hasLesson(lessonId));
        return located instanceof Response
            ? lessonNotFound(located)
            : HttpResponse.json(lessonResponse(located.chapters, lessonId));
    }),
    http.patch("*/api/studio/lessons/:lessonId", async ({ params, request }) => {
        await delay();
        const lessonId = String(params.lessonId);
        const located = editable(locate(hasLesson(lessonId)));
        if (located instanceof Response) {
            return lessonNotFound(located);
        }
        const body = (await request.json()) as LessonRequest;
        const isTaken = located.chapters.some((chapter) =>
            chapter.lessons.some((item) => item.slug === body.slug && item.id !== lessonId),
        );
        if (isTaken) {
            return apiError(409, "SLUG_TAKEN", "Slug đã được bài giảng khác trong khoá dùng");
        }
        const chapters = located.chapters.map((chapter) => ({
            ...chapter,
            lessons: chapter.lessons.map((item) =>
                item.id === lessonId
                    ? lesson(lessonId, body.slug, body.title, body.estimatedMinutes)
                    : item,
            ),
        }));
        writeOutline(located.course.id, chapters);
        sessionStorage.setItem(
            BODIES_KEY,
            JSON.stringify({ ...readBodies(), [lessonId]: body.body }),
        );
        return HttpResponse.json(lessonResponse(chapters, lessonId));
    }),
];
