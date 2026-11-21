import { apiClient } from "@/lib/api-client";

import type {
    CourseReviewAction,
    CourseReviewQueuePage,
    ReviewChapter,
    ReviewCourse,
} from "./types";

export const COURSE_REVIEW_QUEUE_PAGE_SIZE = 50;

function coursePath(courseId: string): string {
    return encodeURIComponent(courseId);
}

export const courseReviewApi = {
    getQueue: (signal?: AbortSignal) =>
        apiClient.get<CourseReviewQueuePage>("/api/admin/courses", {
            params: { page: 1, size: COURSE_REVIEW_QUEUE_PAGE_SIZE },
            signal,
        }),
    getCourse: (courseId: string, signal?: AbortSignal) =>
        apiClient.get<ReviewCourse>(`/api/studio/courses/${coursePath(courseId)}`, { signal }),
    getOutline: (courseId: string, signal?: AbortSignal) =>
        apiClient.get<ReviewChapter[]>(`/api/studio/courses/${coursePath(courseId)}/chapters`, {
            signal,
        }),
    review: (courseId: string, action: CourseReviewAction, reason: string | null) =>
        apiClient.post<ReviewCourse>(
            `/api/admin/courses/${coursePath(courseId)}/${action}`,
            reason === null ? undefined : { reason },
        ),
};
