import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { adminJudgeApi } from "../judge/api";

const JUDGE_KEY = ["admin", "judge"] as const;
const REFRESH_INTERVAL_MS = 10_000;

export function useJudgeQueue() {
    return useQuery({
        queryKey: [...JUDGE_KEY, "queue"],
        queryFn: ({ signal }) => adminJudgeApi.queue(signal),
        refetchInterval: REFRESH_INTERVAL_MS,
    });
}

export function useJudgeAttention() {
    return useQuery({
        queryKey: [...JUDGE_KEY, "attention"],
        queryFn: ({ signal }) => adminJudgeApi.attention(signal),
        refetchInterval: REFRESH_INTERVAL_MS,
    });
}

export function useRejudgeSubmission() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: adminJudgeApi.rejudge,
        onSuccess: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: JUDGE_KEY }),
                queryClient.invalidateQueries({ queryKey: ["admin", "submissions"] }),
                queryClient.invalidateQueries({ queryKey: ["admin", "submission"] }),
            ]),
    });
}
