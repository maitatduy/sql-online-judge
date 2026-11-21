import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { CourseInfo, CreateCourseRequest, StudioCourse } from "@/features/course-studio/types";

import { apiError } from "./api-error";
import { readMockSession, SETTER_SESSION_USER } from "./session";

const STORAGE_KEY = "sqljudge:mock:studio-courses";
const HOUR_MS = 60 * 60_000;

interface StoredCourse extends StudioCourse {
    authorId: string;
}

function seededCourses(): StoredCourse[] {
    const hoursAgo = (hours: number) => new Date(Date.now() - hours * HOUR_MS).toISOString();
    return [
        {
            id: "0192f3f1-0000-7000-8000-000000000001",
            authorId: SETTER_SESSION_USER.id,
            slug: "window-function-tu-dau",
            status: "DRAFT",
            revisionId: "0192f3f1-0000-7000-8000-000000000011",
            revisionStatus: "DRAFT",
            title: "Window function từ đầu",
            summary: "Xếp hạng, cộng dồn và so sánh với dòng trước bằng OVER.",
            difficulty: "MEDIUM",
            estimatedMinutes: 90,
            lessonCount: 4,
            submittedAt: hoursAgo(26),
            latestReview: {
                decision: "CHANGES_REQUESTED",
                reason: "Thêm ví dụ cho bài DENSE_RANK.",
                reviewedAt: hoursAgo(20),
            },
            updatedAt: hoursAgo(2),
        },
        {
            id: "0192f3f1-0000-7000-8000-000000000002",
            authorId: SETTER_SESSION_USER.id,
            slug: "join-trong-thuc-te",
            status: "DRAFT",
            revisionId: "0192f3f1-0000-7000-8000-000000000012",
            revisionStatus: "IN_REVIEW",
            title: "JOIN trong thực tế",
            summary: "Ghép bảng đơn hàng, khách hàng và sản phẩm.",
            difficulty: "EASY",
            estimatedMinutes: 60,
            lessonCount: 3,
            submittedAt: hoursAgo(30),
            latestReview: null,
            updatedAt: hoursAgo(30),
        },
    ];
}

function readCourses(): StoredCourse[] {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as StoredCourse[]) : seededCourses();
}

function writeCourses(courses: StoredCourse[]): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(courses));
}

function publicOf(course: StoredCourse): StudioCourse {
    const publicCourse: Partial<StoredCourse> = { ...course };
    delete publicCourse.authorId;
    return publicCourse as StudioCourse;
}

function courseNotFound() {
    return apiError(404, "COURSE_NOT_FOUND", "Không tìm thấy khoá học");
}

function unauthenticated() {
    return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
}

function ownedCourse(courseId: string, userId: string): StoredCourse | undefined {
    const isAdmin = readMockSession()?.role === "ADMIN";
    return readCourses().find(
        (course) => course.id === courseId && (course.authorId === userId || isAdmin),
    );
}

export const studioCourseHandlers: RequestHandler[] = [
    http.get("*/api/studio/courses", async () => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return unauthenticated();
        }
        return HttpResponse.json(
            readCourses()
                .filter((course) => course.authorId === user.id)
                .sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
                .map(publicOf),
        );
    }),
    http.post("*/api/studio/courses", async ({ request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return unauthenticated();
        }
        const body = (await request.json()) as CreateCourseRequest;
        const courses = readCourses();
        if (courses.some((course) => course.slug === body.slug)) {
            return apiError(409, "SLUG_TAKEN", "Slug đã được khoá khác dùng");
        }
        const created: StoredCourse = {
            ...body,
            id: crypto.randomUUID(),
            authorId: user.id,
            status: "DRAFT",
            revisionId: crypto.randomUUID(),
            revisionStatus: "DRAFT",
            estimatedMinutes: 0,
            lessonCount: 0,
            submittedAt: null,
            latestReview: null,
            updatedAt: new Date().toISOString(),
        };
        writeCourses([created, ...courses]);
        return HttpResponse.json(publicOf(created), { status: 201 });
    }),
    http.get("*/api/studio/courses/:courseId", async ({ params }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return unauthenticated();
        }
        const course = ownedCourse(String(params.courseId), user.id);
        return course ? HttpResponse.json(publicOf(course)) : courseNotFound();
    }),
    http.post("*/api/studio/courses/:courseId/submit", async ({ params }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return unauthenticated();
        }
        const course = ownedCourse(String(params.courseId), user.id);
        if (!course) {
            return courseNotFound();
        }
        if (course.revisionStatus !== "DRAFT") {
            return apiError(422, "COURSE_LOCKED", "Khoá đang chờ duyệt nên chưa sửa được");
        }
        if (course.lessonCount === 0) {
            return apiError(
                422,
                "COURSE_EMPTY",
                "Khoá cần ít nhất một bài giảng trước khi gửi duyệt",
            );
        }
        const submitted: StoredCourse = {
            ...course,
            revisionStatus: "IN_REVIEW",
            submittedAt: new Date().toISOString(),
        };
        writeCourses(readCourses().map((stored) => (stored.id === course.id ? submitted : stored)));
        return HttpResponse.json({ ...publicOf(submitted), latestReview: null });
    }),
    http.patch("*/api/studio/courses/:courseId", async ({ params, request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return unauthenticated();
        }
        const course = ownedCourse(String(params.courseId), user.id);
        if (!course) {
            return courseNotFound();
        }
        if (course.revisionStatus !== "DRAFT") {
            return apiError(422, "COURSE_LOCKED", "Khoá đang chờ duyệt nên chưa sửa được");
        }
        const updated: StoredCourse = {
            ...course,
            ...((await request.json()) as CourseInfo),
            updatedAt: new Date().toISOString(),
        };
        writeCourses(readCourses().map((stored) => (stored.id === course.id ? updated : stored)));
        return HttpResponse.json(publicOf(updated));
    }),
];

export function findOwnedCourse(courseId: string, userId: string): StudioCourse | undefined {
    const course = ownedCourse(courseId, userId);
    return course ? publicOf(course) : undefined;
}

export function updateCourseTotals(courseId: string, lessonCount: number, minutes: number): void {
    writeCourses(
        readCourses().map((course) =>
            course.id === courseId ? { ...course, lessonCount, estimatedMinutes: minutes } : course,
        ),
    );
}

function adminOnly(): Response | null {
    const user = readMockSession();
    if (!user) {
        return unauthenticated();
    }
    return user.role === "ADMIN"
        ? null
        : apiError(403, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này");
}

function pendingCourse(courseId: string): StoredCourse | Response {
    const course = readCourses().find((item) => item.id === courseId);
    if (!course) {
        return courseNotFound();
    }
    return course.revisionStatus === "IN_REVIEW"
        ? course
        : apiError(422, "COURSE_NOT_IN_REVIEW", "Khoá không ở trạng thái chờ duyệt");
}

function saveReviewed(course: StoredCourse): Response {
    writeCourses(readCourses().map((stored) => (stored.id === course.id ? course : stored)));
    return HttpResponse.json(publicOf(course));
}

export const adminCourseReviewHandlers: RequestHandler[] = [
    http.get("*/api/admin/courses", async () => {
        await delay();
        const denied = adminOnly();
        if (denied) {
            return denied;
        }
        const items = readCourses()
            .filter((course) => course.revisionStatus === "IN_REVIEW")
            .sort((left, right) => (left.submittedAt ?? "").localeCompare(right.submittedAt ?? ""))
            .map((course) => ({
                courseId: course.id,
                slug: course.slug,
                title: course.title,
                difficulty: course.difficulty,
                lessonCount: course.lessonCount,
                estimatedMinutes: course.estimatedMinutes,
                authorId: course.authorId,
                submittedAt: course.submittedAt,
            }));
        return HttpResponse.json({
            items,
            page: 1,
            size: 50,
            totalItems: items.length,
            totalPages: 1,
        });
    }),
    http.post("*/api/admin/courses/:courseId/approve", async ({ params }) => {
        await delay();
        const course = adminOnly() ?? pendingCourse(String(params.courseId));
        if (course instanceof Response) {
            return course;
        }
        return saveReviewed({
            ...course,
            status: "PUBLISHED",
            revisionStatus: "PUBLISHED",
            latestReview: {
                decision: "APPROVED",
                reason: null,
                reviewedAt: new Date().toISOString(),
            },
        });
    }),
    http.post("*/api/admin/courses/:courseId/request-changes", async ({ params, request }) => {
        await delay();
        const course = adminOnly() ?? pendingCourse(String(params.courseId));
        if (course instanceof Response) {
            return course;
        }
        const { reason } = (await request.json()) as { reason: string };
        return saveReviewed({
            ...course,
            revisionStatus: "DRAFT",
            latestReview: {
                decision: "CHANGES_REQUESTED",
                reason: reason.trim(),
                reviewedAt: new Date().toISOString(),
            },
        });
    }),
];
