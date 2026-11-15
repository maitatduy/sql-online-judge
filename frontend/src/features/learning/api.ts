import { apiClient } from "@/lib/api-client";
import type { Difficulty } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

import type { CourseDetail, CourseProgress, CourseSummary, Lesson } from "./types";

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
};
