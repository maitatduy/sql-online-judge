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
    RatingSummary,
    RecentSubmission,
    SubmissionStats,
    SuggestedProblem,
    TopicProgressSummary,
    UpcomingContest,
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
    getSubmissionStats: (signal?: AbortSignal) =>
        apiClient.get<SubmissionStats>("/api/submissions/summary", { signal }),
    listRecentSubmissions: (size: number, signal?: AbortSignal) =>
        apiClient.get<RecentSubmission[]>("/api/submissions/recent", { params: { size }, signal }),
    listDailyChallenges: (size: number, signal?: AbortSignal) =>
        apiClient.get<{ items: SuggestedProblem[] }>("/api/problems/daily-challenges", {
            params: { size },
            signal,
        }),
    getTopicProgress: (userId: string, signal?: AbortSignal) =>
        apiClient.get<TopicProgressSummary>(
            `/api/solved-summaries/${encodeURIComponent(userId)}/topics`,
            { signal },
        ),
    getRatingSummary: (userId: string, signal?: AbortSignal) =>
        apiClient.get<RatingSummary>(`/api/leaderboard/${encodeURIComponent(userId)}`, {
            signal,
        }),
    listUpcomingContests: (size: number, signal?: AbortSignal) =>
        apiClient.get<Page<UpcomingContest>>("/api/contests", {
            params: { phase: "UPCOMING", page: 1, size },
            signal,
        }),
};
