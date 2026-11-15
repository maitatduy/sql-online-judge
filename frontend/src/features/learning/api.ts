import { apiClient } from "@/lib/api-client";
import type { Difficulty } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

import type { CourseDetail, CourseSummary } from "./types";

export const learningApi = {
    listCourses: (difficulty: Difficulty | undefined, signal?: AbortSignal) =>
        apiClient.get<Page<CourseSummary>>("/api/courses", {
            params: { difficulty },
            signal,
        }),
    getCourse: (slug: string, signal?: AbortSignal) =>
        apiClient.get<CourseDetail>(`/api/courses/${encodeURIComponent(slug)}`, { signal }),
};
