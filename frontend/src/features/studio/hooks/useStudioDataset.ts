import { useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { DatasetTable, StudioDataset, UpdateDatasetRequest } from "../types";

export interface DatasetChanges {
    info: UpdateDatasetRequest | null;
    tables: DatasetTable[] | null;
}

export function studioDatasetQueryKey(datasetId: string) {
    return ["studio-dataset", datasetId] as const;
}

export function useStudioDataset(datasetId: string) {
    return useQuery({
        queryKey: studioDatasetQueryKey(datasetId),
        queryFn: ({ signal }) => studioApi.getDataset(datasetId, signal),
        retry: false,
    });
}

async function saveChanges(datasetId: string, changes: DatasetChanges): Promise<StudioDataset> {
    let saved: StudioDataset | null = null;
    if (changes.info) {
        saved = await studioApi.updateDataset(datasetId, changes.info);
    }
    if (changes.tables) {
        saved = await studioApi.replaceDatasetSchema(datasetId, changes.tables);
    }
    if (!saved) {
        throw new Error("Không có thay đổi nào để lưu");
    }
    return saved;
}

export function useSaveDataset(datasetId: string, onError: (error: Error) => void) {
    const queryClient = useQueryClient();
    const [savedAt, setSavedAt] = useState<string | null>(null);
    const mutation = useMutation({
        mutationFn: (changes: DatasetChanges) => saveChanges(datasetId, changes),
        onSuccess: (dataset: StudioDataset) => {
            setSavedAt(new Date().toISOString());
            queryClient.setQueryData(studioDatasetQueryKey(datasetId), dataset);
            void queryClient.invalidateQueries({ queryKey: ["studio-datasets"] });
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
