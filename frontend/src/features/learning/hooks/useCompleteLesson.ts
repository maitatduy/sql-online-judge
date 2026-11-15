import { useMutation, useQueryClient } from "@tanstack/react-query";

import { learningApi } from "../api";
import { courseProgressKey } from "./useCourseProgress";

export function useCompleteLesson(courseSlug: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (lessonSlug: string) => learningApi.completeLesson(courseSlug, lessonSlug),
        onSuccess: (progress) => queryClient.setQueryData(courseProgressKey(courseSlug), progress),
    });
}
