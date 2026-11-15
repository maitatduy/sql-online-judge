import { useMutation, useQuery } from "@tanstack/react-query";
import { useState } from "react";

import { learningApi } from "../api";

const RUN_POLL_INTERVAL_MS = 1000;

export function useLessonRun(cellKey: string) {
    const [runId, setRunId] = useState<string | null>(null);
    const start = useMutation({
        mutationFn: (sql: string) => learningApi.startLessonRun(cellKey, sql),
        onMutate: () => setRunId(null),
        onSuccess: (accepted) => setRunId(accepted.runId),
    });
    const runQuery = useQuery({
        queryKey: ["lesson-run", runId],
        queryFn: ({ signal }) => learningApi.getLessonRun(runId ?? "", signal),
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
