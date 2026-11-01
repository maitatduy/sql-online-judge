import { useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { StudioDataset } from "../types";
import { studioDatasetQueryKey } from "./useStudioDataset";

interface VariantTableRef {
    datasetId: string;
    variantId: string;
    tableName: string;
}

function variantCsvQueryKey({ datasetId, variantId, tableName }: VariantTableRef) {
    return ["studio-dataset", datasetId, "csv", variantId, tableName] as const;
}

export function useVariantCsv(ref: VariantTableRef, hasData: boolean) {
    return useQuery({
        queryKey: variantCsvQueryKey(ref),
        queryFn: ({ signal }) =>
            studioApi.getVariantCsv(ref.datasetId, ref.variantId, ref.tableName, signal),
        enabled: hasData,
        retry: false,
    });
}

export function useSaveVariantCsv(ref: VariantTableRef) {
    const queryClient = useQueryClient();
    const [savedAt, setSavedAt] = useState<string | null>(null);
    const mutation = useMutation({
        mutationFn: (csv: string) =>
            studioApi.saveVariantCsv(ref.datasetId, ref.variantId, ref.tableName, csv),
        onSuccess: (dataset: StudioDataset) => {
            setSavedAt(new Date().toISOString());
            queryClient.setQueryData(studioDatasetQueryKey(ref.datasetId), dataset);
            void queryClient.invalidateQueries({ queryKey: variantCsvQueryKey(ref) });
        },
    });

    return {
        save: mutation.mutate,
        isSaving: mutation.isPending,
        error: mutation.error,
        savedAt,
    };
}
