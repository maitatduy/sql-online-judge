import { useState } from "react";

import { useMutation, useQuery } from "@tanstack/react-query";

import { practiceApi } from "../api";
import type { StartRunRequest } from "../types";

const RUN_POLL_INTERVAL_MS = 1000;

export function useCodeRun(problemId: string) {
    const [runId, setRunId] = useState<string | null>(null);
    const start = useMutation({
        mutationFn: (request: StartRunRequest) => practiceApi.startRun(problemId, request),
        onMutate: () => setRunId(null),
        onSuccess: (accepted) => setRunId(accepted.runId),
    });
    const runQuery = useQuery({
        queryKey: ["run", runId],
        queryFn: ({ signal }) => practiceApi.getRun(runId ?? "", signal),
        enabled: runId !== null,
        staleTime: Infinity,
        refetchInterval: (query) =>
            query.state.data?.status === "PENDING" ? RUN_POLL_INTERVAL_MS : false,
    });

    const run = runId === null ? null : (runQuery.data ?? null);
    const isRunning =
        start.isPending || (runId !== null && runQuery.isPending) || run?.status === "PENDING";

    return {
        run: run?.status === "COMPLETED" ? run : null,
        isRunning,
        error: start.error ?? (runId === null ? null : runQuery.error),
        startRun: start.mutate,
    };
}
