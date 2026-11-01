import { useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { DatasetTable, PendingImport, StudioDataset, UpdateDatasetRequest } from "../types";

export interface DatasetChanges {
    info: UpdateDatasetRequest | null;
    tables: DatasetTable[] | null;
    pendingImport: PendingImport | null;
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

function variantIdOf(dataset: StudioDataset, variantName: string): string {
    const variant = dataset.variants.find((candidate) => candidate.name === variantName);
    if (!variant) {
        throw new Error(`Không tìm thấy bộ dữ liệu ${variantName}`);
    }
    return variant.id;
}

async function saveImport(
    datasetId: string,
    current: StudioDataset,
    pendingImport: PendingImport,
): Promise<StudioDataset> {
    let saved = pendingImport.createsVariant
        ? await studioApi.addDatasetVariant(datasetId, {
              name: pendingImport.variantName,
              isPublic: false,
          })
        : current;
    for (const table of pendingImport.data) {
        saved = await studioApi.saveVariantCsv(
            datasetId,
            variantIdOf(saved, pendingImport.variantName),
            table.tableName,
            table.csv,
        );
    }
    return saved;
}

async function saveChanges(
    dataset: StudioDataset,
    changes: DatasetChanges,
): Promise<StudioDataset> {
    let saved = dataset;
    if (changes.info) {
        saved = await studioApi.updateDataset(dataset.id, changes.info);
    }
    if (changes.tables) {
        saved = await studioApi.replaceDatasetSchema(dataset.id, changes.tables);
    }
    if (changes.pendingImport) {
        saved = await saveImport(dataset.id, saved, changes.pendingImport);
    }
    return saved;
}

export function useSaveDataset(dataset: StudioDataset, onError: (error: Error) => void) {
    const queryClient = useQueryClient();
    const [savedAt, setSavedAt] = useState<string | null>(null);
    const mutation = useMutation({
        mutationFn: (changes: DatasetChanges) => saveChanges(dataset, changes),
        onSuccess: (saved: StudioDataset) => {
            setSavedAt(new Date().toISOString());
            queryClient.setQueryData(studioDatasetQueryKey(saved.id), saved);
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
