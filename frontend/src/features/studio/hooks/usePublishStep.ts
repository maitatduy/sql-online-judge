import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiRequestError } from "@/lib/api-client";

import { studioApi } from "../api";
import type { AnswerCheck, StudioProblem } from "../types";
import { studioProblemQueryKey } from "./useStudioProblem";

export function useLatestAnswerCheck(problemId: string) {
    return useQuery({
        queryKey: ["studio-answer-check", problemId, "latest"],
        queryFn: async ({ signal }): Promise<AnswerCheck | null> => {
            try {
                return await studioApi.getLatestAnswerCheck(problemId, signal);
            } catch (error) {
                if (error instanceof ApiRequestError && error.status === 404) {
                    return null;
                }
                throw error;
            }
        },
        retry: false,
    });
}

export function useSubmitForReview(problemId: string, onSubmitted: () => void) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: () => studioApi.submitForReview(problemId),
        onSuccess: (problem: StudioProblem) => {
            queryClient.setQueryData(studioProblemQueryKey(problemId), problem);
            void queryClient.invalidateQueries({ queryKey: ["studio-problems"] });
            onSubmitted();
        },
    });
}
