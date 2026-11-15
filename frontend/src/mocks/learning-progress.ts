import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { CourseDetail, CourseProgress } from "@/features/learning/types";

import { apiError } from "./api-error";
import { LEARNING_COURSES } from "./learning-fixtures";
import { readMockSession } from "./session";

const enrolledCourses = new Set<string>();
const completedLessons = new Set<string>();

export function resetMockLearningProgress(): void {
    enrolledCourses.clear();
    completedLessons.clear();
}

export function completeMockLesson(courseSlug: string, lessonKey: string): void {
    enrolledCourses.add(courseSlug);
    completedLessons.add(lessonKey);
}

function progressOf(course: CourseDetail): CourseProgress {
    const lessons = course.chapters.flatMap((chapter) => chapter.lessons);
    return {
        enrolled: enrolledCourses.has(course.slug),
        completedLessonKeys: lessons
            .map((lesson) => lesson.lessonKey)
            .filter((lessonKey) => completedLessons.has(lessonKey)),
        lessonCount: lessons.length,
        nextLessonSlug:
            lessons.find((lesson) => !completedLessons.has(lesson.lessonKey))?.slug ?? null,
    };
}

function withCourse(slug: unknown, respond: (course: CourseDetail) => Response): Response {
    if (!readMockSession()) {
        return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
    }
    const course = LEARNING_COURSES.find((candidate) => candidate.slug === slug);
    if (!course) {
        return apiError(404, "COURSE_NOT_FOUND", "Không tìm thấy khoá học");
    }
    return respond(course);
}

export const learningProgressHandlers: RequestHandler[] = [
    http.get("*/api/courses/:slug/progress", async ({ params }) => {
        await delay(100);
        return withCourse(params.slug, (course) => HttpResponse.json(progressOf(course)));
    }),
    http.post("*/api/courses/:slug/enrollment", async ({ params }) => {
        await delay(100);
        return withCourse(params.slug, (course) => {
            enrolledCourses.add(course.slug);
            return HttpResponse.json(progressOf(course));
        });
    }),
    http.post("*/api/courses/:slug/lessons/:lessonSlug/completion", async ({ params }) => {
        await delay(100);
        return withCourse(params.slug, (course) => {
            const lesson = course.chapters
                .flatMap((chapter) => chapter.lessons)
                .find((candidate) => candidate.slug === params.lessonSlug);
            if (!lesson) {
                return apiError(404, "LESSON_NOT_FOUND", "Không tìm thấy bài giảng");
            }
            completeMockLesson(course.slug, lesson.lessonKey);
            return HttpResponse.json(progressOf(course));
        });
    }),
];
