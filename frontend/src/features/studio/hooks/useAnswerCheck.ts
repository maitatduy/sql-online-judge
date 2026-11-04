import { useMutation, useQuery } from "@tanstack/react-query";

import { studioApi } from "../api";

export const CHECK_POLL_INTERVAL_MS = 1000;

export function useStartAnswerCheck(problemId: string) {
    return useMutation({
        mutationFn: () => studioApi.startAnswerCheck(problemId),
    });
}

export function useAnswerCheck(problemId: string, checkId: string | null) {
    return useQuery({
        queryKey: ["studio-answer-check", problemId, checkId],
        queryFn: ({ signal }) => studioApi.getAnswerCheck(problemId, checkId ?? "", signal),
        enabled: checkId !== null,
        refetchInterval: (query) =>
            query.state.data?.status === "PENDING" || query.state.data === undefined
                ? CHECK_POLL_INTERVAL_MS
                : false,
    });
}
