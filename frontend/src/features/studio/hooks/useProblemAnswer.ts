import { useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { ProblemAnswer, SaveProblemAnswerRequest } from "../types";

export function problemAnswerQueryKey(problemId: string) {
    return ["studio-problem-answer", problemId] as const;
}

export function useProblemAnswer(problemId: string, enabled: boolean) {
    return useQuery({
        queryKey: problemAnswerQueryKey(problemId),
        queryFn: ({ signal }) => studioApi.getProblemAnswer(problemId, signal),
        enabled,
        retry: false,
    });
}

export function useSaveProblemAnswer(problemId: string, onError: (error: Error) => void) {
    const queryClient = useQueryClient();
    const [savedAt, setSavedAt] = useState<string | null>(null);
    const mutation = useMutation({
        mutationFn: (request: SaveProblemAnswerRequest) =>
            studioApi.saveProblemAnswer(problemId, request),
        onSuccess: (answer: ProblemAnswer) => {
            setSavedAt(new Date().toISOString());
            queryClient.setQueryData(problemAnswerQueryKey(problemId), answer);
        },
        onError,
    });

    return {
        save: mutation.mutate,
        isSaving: mutation.isPending,
        error: mutation.error,
        savedAt,
    };
}
