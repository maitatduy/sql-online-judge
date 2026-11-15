import { useMutation, useQueryClient } from "@tanstack/react-query";

import { learningApi } from "../api";
import type { QuizAnswer } from "../types";
import { courseProgressKey } from "./useCourseProgress";

export function useSubmitQuiz(courseSlug: string, lessonSlug: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (answers: QuizAnswer[]) =>
            learningApi.submitQuiz(courseSlug, lessonSlug, answers),
        onSuccess: (result) =>
            queryClient.setQueryData(courseProgressKey(courseSlug), result.progress),
    });
}
