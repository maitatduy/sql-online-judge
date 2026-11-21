import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

import type {
    CellRequest,
    CourseInfo,
    CreateCourseRequest,
    DatasetChoice,
    DatasetOption,
    LessonRequest,
    QuizQuestionRequest,
    StudioCell,
    StudioChapter,
    StudioCourse,
    StudioLesson,
    StudioQuizQuestion,
} from "./types";

function coursePath(courseId: string): string {
    return `/api/studio/courses/${encodeURIComponent(courseId)}`;
}

function chapterPath(chapterId: string): string {
    return `/api/studio/chapters/${encodeURIComponent(chapterId)}`;
}

const MAX_DATASET_OPTIONS = 100;

function quizQuestionPath(questionId: string): string {
    return `/api/studio/quiz-questions/${encodeURIComponent(questionId)}`;
}

function cellPath(cellId: string): string {
    return `/api/studio/cells/${encodeURIComponent(cellId)}`;
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
    submitCourse: (courseId: string) =>
        apiClient.post<StudioCourse>(`${coursePath(courseId)}/submit`),
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
    listCells: (lessonId: string, signal?: AbortSignal) =>
        apiClient.get<StudioCell[]>(`${lessonPath(lessonId)}/cells`, { signal }),
    addCell: (lessonId: string, request: CellRequest) =>
        apiClient.post<StudioCell>(`${lessonPath(lessonId)}/cells`, request),
    updateCell: (cellId: string, request: CellRequest) =>
        apiClient.patch<StudioCell>(cellPath(cellId), request),
    deleteCell: (cellId: string) => apiClient.delete<void>(cellPath(cellId)),
    listDatasets: (signal?: AbortSignal) =>
        apiClient.get<Page<DatasetOption>>("/api/studio/datasets", {
            params: { page: 1, size: MAX_DATASET_OPTIONS, sort: "updatedAt,desc" },
            signal,
        }),
    getDataset: (datasetId: string, signal?: AbortSignal) =>
        apiClient.get<DatasetChoice>(`/api/studio/datasets/${encodeURIComponent(datasetId)}`, {
            signal,
        }),
    listQuizQuestions: (lessonId: string, signal?: AbortSignal) =>
        apiClient.get<StudioQuizQuestion[]>(`${lessonPath(lessonId)}/quiz-questions`, { signal }),
    addQuizQuestion: (lessonId: string, request: QuizQuestionRequest) =>
        apiClient.post<StudioQuizQuestion>(`${lessonPath(lessonId)}/quiz-questions`, request),
    replaceQuizQuestion: (questionId: string, request: QuizQuestionRequest) =>
        apiClient.put<StudioQuizQuestion>(quizQuestionPath(questionId), request),
    deleteQuizQuestion: (questionId: string) =>
        apiClient.delete<void>(quizQuestionPath(questionId)),
};
