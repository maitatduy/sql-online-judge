import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { courseStudioApi } from "../api";
import type { LessonRequest } from "../types";
import { STUDIO_COURSES_KEY } from "./useMyCourses";

export function useCourseOutline(courseId: string) {
    return useQuery({
        queryKey: [...STUDIO_COURSES_KEY, "outline", courseId],
        queryFn: ({ signal }) => courseStudioApi.listChapters(courseId, signal),
    });
}

function useOutlineMutation<TVariables, TResult>(
    mutationFn: (variables: TVariables) => Promise<TResult>,
) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: STUDIO_COURSES_KEY }),
    });
}

export function useAddChapter(courseId: string) {
    return useOutlineMutation((title: string) => courseStudioApi.addChapter(courseId, title));
}

export function useRenameChapter() {
    return useOutlineMutation(({ chapterId, title }: { chapterId: string; title: string }) =>
        courseStudioApi.renameChapter(chapterId, title),
    );
}

export function useDeleteChapter() {
    return useOutlineMutation((chapterId: string) => courseStudioApi.deleteChapter(chapterId));
}

export function useAddLesson(chapterId: string) {
    return useOutlineMutation((request: LessonRequest) =>
        courseStudioApi.addLesson(chapterId, request),
    );
}

export function useDeleteLesson() {
    return useOutlineMutation((lessonId: string) => courseStudioApi.deleteLesson(lessonId));
}
