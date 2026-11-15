import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    CourseDetail,
    CourseProgress,
    QuizAnswer,
    QuizResult,
} from "@/features/learning/types";

import { vietnamDateKey } from "@/lib/activity-calendar";

import { apiError } from "./api-error";
import {
    LEARNING_COURSES,
    LEARNING_QUIZ,
    QUIZ_CORRECT_OPTION,
    QUIZ_LESSON_SLUG,
} from "./learning-fixtures";
import { readMockSession } from "./session";

const enrolledCourses = new Set<string>();
const completedLessons = new Set<string>();
const completionDates: string[] = [];

export function resetMockLearningProgress(): void {
    enrolledCourses.clear();
    completedLessons.clear();
    completionDates.length = 0;
}

export function completeMockLesson(courseSlug: string, lessonKey: string): void {
    enrolledCourses.add(courseSlug);
    if (!completedLessons.has(lessonKey)) {
        completionDates.push(vietnamDateKey(new Date()));
    }
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
    http.get("*/api/enrollments", async () => {
        await delay(100);
        if (!readMockSession()) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        return HttpResponse.json(
            LEARNING_COURSES.filter((course) => enrolledCourses.has(course.slug)).map((course) => {
                const progress = progressOf(course);
                return {
                    slug: course.slug,
                    title: course.title,
                    difficulty: course.difficulty,
                    completedCount: progress.completedLessonKeys.length,
                    lessonCount: progress.lessonCount,
                    nextLessonSlug: progress.nextLessonSlug,
                };
            }),
        );
    }),
    http.get("*/api/learning/activity", async () => {
        await delay(100);
        if (!readMockSession()) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const counts = new Map<string, number>();
        completionDates.forEach((date) => counts.set(date, (counts.get(date) ?? 0) + 1));
        return HttpResponse.json(
            [...counts.entries()].sort().map(([date, count]) => ({ date, count })),
        );
    }),
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
    http.post(
        "*/api/courses/:slug/lessons/:lessonSlug/quiz-attempts",
        async ({ params, request }) => {
            await delay(100);
            const { answers } = (await request.json()) as { answers: QuizAnswer[] };
            return withCourse(params.slug, (course) =>
                params.lessonSlug === QUIZ_LESSON_SLUG
                    ? HttpResponse.json(gradeQuiz(course, answers))
                    : apiError(422, "LESSON_HAS_NO_QUIZ", "Bài giảng này không có câu hỏi"),
            );
        },
    ),
];

function gradeQuiz(course: CourseDetail, answers: QuizAnswer[]): QuizResult {
    const results = LEARNING_QUIZ.map((question) => {
        const chosen = answers.find((answer) => answer.questionKey === question.questionKey);
        return {
            questionKey: question.questionKey,
            correct:
                chosen?.optionKeys.length === 1 && chosen.optionKeys[0] === QUIZ_CORRECT_OPTION,
        };
    });
    const allCorrect = results.every((result) => result.correct);
    const lesson = course.chapters
        .flatMap((chapter) => chapter.lessons)
        .find((candidate) => candidate.slug === QUIZ_LESSON_SLUG);
    if (allCorrect && lesson) {
        completeMockLesson(course.slug, lesson.lessonKey);
    }
    return { allCorrect, results, progress: progressOf(course) };
}
