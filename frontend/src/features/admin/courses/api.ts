import { apiClient } from "@/lib/api-client";

import type { AdminCourseAction, AdminCoursePage, AdminCourseStatus } from "./types";

export const ADMIN_COURSES_PAGE_SIZE = 100;

export const adminCourseApi = {
    getCatalog: (status: AdminCourseStatus | null, signal?: AbortSignal) =>
        apiClient.get<AdminCoursePage>("/api/admin/courses/catalog", {
            params: { status: status ?? undefined, page: 1, size: ADMIN_COURSES_PAGE_SIZE },
            signal,
        }),
    changeVisibility: (courseId: string, action: AdminCourseAction) =>
        apiClient.post<unknown>(`/api/admin/courses/${encodeURIComponent(courseId)}/${action}`),
};
