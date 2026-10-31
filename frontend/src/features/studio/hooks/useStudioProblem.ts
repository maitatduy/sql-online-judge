import { useQuery } from "@tanstack/react-query";

import { studioApi } from "../api";

const TAGS_STALE_TIME_MS = 60 * 60_000;

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

export function useStudioTags() {
    return useQuery({
        queryKey: ["tags"],
        queryFn: ({ signal }) => studioApi.listTags(signal),
        staleTime: TAGS_STALE_TIME_MS,
    });
}
