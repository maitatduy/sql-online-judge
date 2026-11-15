import { apiClient } from "@/lib/api-client";

import type { CourseInfo, CreateCourseRequest, StudioCourse } from "./types";

function coursePath(courseId: string): string {
    return `/api/studio/courses/${encodeURIComponent(courseId)}`;
}

export const courseStudioApi = {
    listMyCourses: (signal?: AbortSignal) =>
        apiClient.get<StudioCourse[]>("/api/studio/courses", { signal }),
    createCourse: (request: CreateCourseRequest) =>
        apiClient.post<StudioCourse>("/api/studio/courses", request),
    getCourse: (courseId: string, signal?: AbortSignal) =>
        apiClient.get<StudioCourse>(coursePath(courseId), { signal }),
    updateCourse: (courseId: string, info: CourseInfo) =>
        apiClient.patch<StudioCourse>(coursePath(courseId), info),
};
