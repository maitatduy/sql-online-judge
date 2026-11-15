import type { ActivityDay } from "@/lib/activity-calendar";
import { apiClient } from "@/lib/api-client";
import type { Difficulty } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

import type {
    CourseDetail,
    CourseProgress,
    CourseSummary,
    EnrolledCourse,
    Lesson,
    LessonRun,
    QuizAnswer,
    QuizResult,
    RecentSubmission,
    SubmissionStats,
    SuggestedProblem,
} from "./types";

export const learningApi = {
    listCourses: (difficulty: Difficulty | undefined, signal?: AbortSignal) =>
        apiClient.get<Page<CourseSummary>>("/api/courses", {
            params: { difficulty },
            signal,
        }),
    getCourse: (slug: string, signal?: AbortSignal) =>
        apiClient.get<CourseDetail>(`/api/courses/${encodeURIComponent(slug)}`, { signal }),
    getLesson: (courseSlug: string, lessonSlug: string, signal?: AbortSignal) =>
        apiClient.get<Lesson>(
            `/api/courses/${encodeURIComponent(courseSlug)}/lessons/${encodeURIComponent(lessonSlug)}`,
            { signal },
        ),
    getProgress: (courseSlug: string, signal?: AbortSignal) =>
        apiClient.get<CourseProgress>(`/api/courses/${encodeURIComponent(courseSlug)}/progress`, {
            signal,
        }),
    completeLesson: (courseSlug: string, lessonSlug: string) =>
        apiClient.post<CourseProgress>(
            `/api/courses/${encodeURIComponent(courseSlug)}/lessons/${encodeURIComponent(lessonSlug)}/completion`,
        ),
    startLessonRun: (cellKey: string, sql: string) =>
        apiClient.post<{ runId: string }>(`/api/lesson-cells/${encodeURIComponent(cellKey)}/runs`, {
            sql,
        }),
    getLessonRun: (runId: string, signal?: AbortSignal) =>
        apiClient.get<LessonRun>(`/api/lesson-runs/${encodeURIComponent(runId)}`, { signal }),
    submitQuiz: (courseSlug: string, lessonSlug: string, answers: QuizAnswer[]) =>
        apiClient.post<QuizResult>(
            `/api/courses/${encodeURIComponent(courseSlug)}/lessons/${encodeURIComponent(lessonSlug)}/quiz-attempts`,
            { answers },
        ),
    listEnrollments: (signal?: AbortSignal) =>
        apiClient.get<EnrolledCourse[]>("/api/enrollments", { signal }),
    getLearningActivity: (from: string, signal?: AbortSignal) =>
        apiClient.get<ActivityDay[]>("/api/learning/activity", { params: { from }, signal }),
    getSubmissionStats: (signal?: AbortSignal) =>
        apiClient.get<SubmissionStats>("/api/submissions/summary", { signal }),
    getSubmissionActivity: (from: string, signal?: AbortSignal) =>
        apiClient.get<ActivityDay[]>("/api/submissions/activity", { params: { from }, signal }),
    listRecentSubmissions: (size: number, signal?: AbortSignal) =>
        apiClient.get<RecentSubmission[]>("/api/submissions/recent", { params: { size }, signal }),
    listSuggestedProblems: (size: number, signal?: AbortSignal) =>
        apiClient.get<Page<SuggestedProblem>>("/api/problems", {
            params: { progress: "NOT_STARTED", size },
            signal,
        }),
};
