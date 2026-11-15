import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { CourseDetail, CourseSummary, Lesson } from "@/features/learning/types";
import { DIFFICULTIES } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

import { validationFailed } from "./admin-user-errors";
import { apiError } from "./api-error";
import { LEARNING_COURSES } from "./learning-fixtures";
import { readMockSession } from "./session";

const PAGE_SIZE = 20;

function lessonOf(courseSlug: string, lessonSlug: string): Lesson | undefined {
    const course = LEARNING_COURSES.find((candidate) => candidate.slug === courseSlug);
    for (const chapter of course?.chapters ?? []) {
        const lesson = chapter.lessons.find((candidate) => candidate.slug === lessonSlug);
        if (course && lesson) {
            return {
                ...lesson,
                body: `## ${lesson.title}\n\nVí dụ:\n\n\`\`\`sql\nSELECT name, salary FROM employees;\n\`\`\``,
                chapterTitle: chapter.title,
                courseSlug: course.slug,
                courseTitle: course.title,
                cells: [
                    {
                        cellKey: `cell-${lesson.lessonKey}`,
                        starterSql: "SELECT id, name FROM departments;",
                        dialect: "POSTGRESQL",
                    },
                ],
            };
        }
    }
    return undefined;
}

function summaryOf(course: CourseDetail): CourseSummary {
    return {
        id: course.id,
        slug: course.slug,
        title: course.title,
        summary: course.summary,
        difficulty: course.difficulty,
        lessonCount: course.lessonCount,
        estimatedMinutes: course.estimatedMinutes,
    };
}

export const learningHandlers: RequestHandler[] = [
    http.get("*/api/courses", async ({ request }) => {
        await delay(150);
        const difficulty = new URL(request.url).searchParams.get("difficulty");
        if (difficulty !== null && !(DIFFICULTIES as readonly string[]).includes(difficulty)) {
            return validationFailed({ field: "difficulty", message: "Độ khó không hợp lệ" });
        }
        const items = [...LEARNING_COURSES]
            .filter((course) => difficulty === null || course.difficulty === difficulty)
            .sort((left, right) => right.publishedAt.localeCompare(left.publishedAt))
            .map(summaryOf);
        const page: Page<CourseSummary> = {
            items,
            page: 1,
            size: PAGE_SIZE,
            totalItems: items.length,
            totalPages: items.length === 0 ? 0 : 1,
        };
        return HttpResponse.json(page);
    }),
    http.get("*/api/courses/:slug", async ({ params }) => {
        await delay(150);
        const course = LEARNING_COURSES.find((candidate) => candidate.slug === params.slug);
        if (!course) {
            return apiError(404, "COURSE_NOT_FOUND", "Không tìm thấy khoá học");
        }
        return HttpResponse.json(course);
    }),
    http.get("*/api/courses/:slug/lessons/:lessonSlug", async ({ params }) => {
        await delay(150);
        if (!readMockSession()) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const lesson = lessonOf(String(params.slug), String(params.lessonSlug));
        if (!lesson) {
            return apiError(404, "LESSON_NOT_FOUND", "Không tìm thấy bài giảng");
        }
        return HttpResponse.json(lesson);
    }),
];
