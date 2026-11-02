import { useMutation, useQuery } from "@tanstack/react-query";

import type { CompletionSchema } from "@/lib/sql-completion";

import { studioApi } from "../api";
import type { CreateStudioRunRequest, StudioDataset } from "../types";
import { studioDatasetQueryKey } from "./useStudioDataset";

export const RUN_POLL_INTERVAL_MS = 1000;

export function useCreateStudioRun(problemId: string) {
    return useMutation({
        mutationFn: (request: CreateStudioRunRequest) =>
            studioApi.createStudioRun(problemId, request),
    });
}

function completionSchemaOf(dataset: StudioDataset): CompletionSchema {
    return {
        tables: dataset.schema.tables.map((table) => ({
            name: table.name,
            columns: table.columns.map((column) => column.name),
        })),
    };
}

export function useCompletionSchema(datasetId: string | null) {
    return useQuery({
        queryKey: studioDatasetQueryKey(datasetId ?? ""),
        queryFn: ({ signal }) => studioApi.getDataset(datasetId ?? "", signal),
        enabled: datasetId !== null,
        select: completionSchemaOf,
        retry: false,
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
