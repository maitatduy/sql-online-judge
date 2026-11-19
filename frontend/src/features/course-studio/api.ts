import { apiClient } from "@/lib/api-client";

import type {
    CourseInfo,
    CreateCourseRequest,
    LessonRequest,
    StudioChapter,
    StudioCourse,
    StudioLesson,
} from "./types";

function coursePath(courseId: string): string {
    return `/api/studio/courses/${encodeURIComponent(courseId)}`;
}

function chapterPath(chapterId: string): string {
    return `/api/studio/chapters/${encodeURIComponent(chapterId)}`;
}

function lessonPath(lessonId: string): string {
    return `/api/studio/lessons/${encodeURIComponent(lessonId)}`;
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
    listChapters: (courseId: string, signal?: AbortSignal) =>
        apiClient.get<StudioChapter[]>(`${coursePath(courseId)}/chapters`, { signal }),
    addChapter: (courseId: string, title: string) =>
        apiClient.post<StudioChapter>(`${coursePath(courseId)}/chapters`, { title }),
    renameChapter: (chapterId: string, title: string) =>
        apiClient.patch<StudioChapter>(chapterPath(chapterId), { title }),
    deleteChapter: (chapterId: string) => apiClient.delete<void>(chapterPath(chapterId)),
    addLesson: (chapterId: string, request: LessonRequest) =>
        apiClient.post<StudioLesson>(`${chapterPath(chapterId)}/lessons`, request),
    getLesson: (lessonId: string, signal?: AbortSignal) =>
        apiClient.get<StudioLesson>(lessonPath(lessonId), { signal }),
    updateLesson: (lessonId: string, request: LessonRequest) =>
        apiClient.patch<StudioLesson>(lessonPath(lessonId), request),
    deleteLesson: (lessonId: string) => apiClient.delete<void>(lessonPath(lessonId)),
};
