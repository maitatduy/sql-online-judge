import { useMutation, useQuery } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { CreateStudioRunRequest } from "../types";

export const RUN_POLL_INTERVAL_MS = 1000;

export function useCreateStudioRun(problemId: string) {
    return useMutation({
        mutationFn: (request: CreateStudioRunRequest) =>
            studioApi.createStudioRun(problemId, request),
    });
}

export function useStudioRun(problemId: string, runId: string | null) {
    return useQuery({
        queryKey: ["studio-run", problemId, runId],
        queryFn: ({ signal }) => studioApi.getStudioRun(problemId, runId ?? "", signal),
        enabled: runId !== null,
        refetchInterval: (query) =>
            query.state.data?.status === "COMPLETED" ? false : RUN_POLL_INTERVAL_MS,
    });
}
