import { useMutation, useQuery } from "@tanstack/react-query";
import { useState } from "react";

import type { Dialect } from "@/lib/dialect";

import { communityApi } from "../api";

const RUN_POLL_INTERVAL_MS = 1000;

export function useProblemRun(problemId: string, dialect: Dialect | null) {
    const [runId, setRunId] = useState<string | null>(null);
    const start = useMutation({
        mutationFn: (code: string) =>
            communityApi.startRun({ problemId, dialect: dialect ?? "POSTGRESQL", code }),
        onMutate: () => setRunId(null),
        onSuccess: (accepted) => setRunId(accepted.runId),
    });
    const runQuery = useQuery({
        queryKey: ["community", "run", runId],
        queryFn: ({ signal }) => communityApi.run(runId ?? "", signal),
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
