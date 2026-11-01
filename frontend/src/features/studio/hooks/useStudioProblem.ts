import { useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { StudioProblem, UpdateProblemRequest } from "../types";

const TAGS_STALE_TIME_MS = 60 * 60_000;

export interface ProblemSave {
    save: (change: UpdateProblemRequest) => void;
    isSaving: boolean;
    error: Error | null;
    savedAt: string | null;
}

export function studioProblemQueryKey(problemId: string) {
    return ["studio-problem", problemId] as const;
}

export function useStudioProblem(problemId: string) {
    return useQuery({
        queryKey: studioProblemQueryKey(problemId),
        queryFn: ({ signal }) => studioApi.getProblem(problemId, signal),
        retry: false,
    });
}

export function useSaveProblem(problemId: string, onError: (error: Error) => void): ProblemSave {
    const queryClient = useQueryClient();
    const [savedAt, setSavedAt] = useState<string | null>(null);
    const mutation = useMutation({
        mutationFn: (change: UpdateProblemRequest) => studioApi.updateProblem(problemId, change),
        onSuccess: (problem: StudioProblem) => {
            setSavedAt(new Date().toISOString());
            queryClient.setQueryData(studioProblemQueryKey(problemId), problem);
            void queryClient.invalidateQueries({ queryKey: ["studio-problems"] });
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

export function useStudioTags() {
    return useQuery({
        queryKey: ["tags"],
        queryFn: ({ signal }) => studioApi.listTags(signal),
        staleTime: TAGS_STALE_TIME_MS,
    });
}
