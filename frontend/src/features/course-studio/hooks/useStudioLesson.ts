import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { courseStudioApi } from "../api";
import type { LessonRequest } from "../types";
import { STUDIO_COURSES_KEY } from "./useMyCourses";

function lessonKey(lessonId: string) {
    return [...STUDIO_COURSES_KEY, "lesson", lessonId] as const;
}

export function useStudioLesson(lessonId: string) {
    return useQuery({
        queryKey: lessonKey(lessonId),
        queryFn: ({ signal }) => courseStudioApi.getLesson(lessonId, signal),
    });
}

export function useUpdateLesson(lessonId: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (request: LessonRequest) => courseStudioApi.updateLesson(lessonId, request),
        onSuccess: (lesson) => {
            queryClient.setQueryData(lessonKey(lessonId), lesson);
            return queryClient.invalidateQueries({
                queryKey: STUDIO_COURSES_KEY,
                predicate: (query) => query.queryKey[1] !== "lesson",
            });
        },
    });
}
