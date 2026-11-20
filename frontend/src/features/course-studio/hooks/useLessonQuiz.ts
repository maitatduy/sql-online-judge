import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { courseStudioApi } from "../api";
import type { QuizQuestionRequest } from "../types";
import { STUDIO_COURSES_KEY } from "./useMyCourses";

function quizKey(lessonId: string) {
    return [...STUDIO_COURSES_KEY, "quiz", lessonId] as const;
}

export function useLessonQuiz(lessonId: string) {
    return useQuery({
        queryKey: quizKey(lessonId),
        queryFn: ({ signal }) => courseStudioApi.listQuizQuestions(lessonId, signal),
    });
}

function useQuizMutation<TVariables, TResult>(
    lessonId: string,
    mutationFn: (variables: TVariables) => Promise<TResult>,
) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: quizKey(lessonId) }),
    });
}

export function useSaveQuizQuestion(lessonId: string, questionId?: string) {
    return useQuizMutation(lessonId, (request: QuizQuestionRequest) =>
        questionId
            ? courseStudioApi.replaceQuizQuestion(questionId, request)
            : courseStudioApi.addQuizQuestion(lessonId, request),
    );
}

export function useDeleteQuizQuestion(lessonId: string) {
    return useQuizMutation(lessonId, (questionId: string) =>
        courseStudioApi.deleteQuizQuestion(questionId),
    );
}
