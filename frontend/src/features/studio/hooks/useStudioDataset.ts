import { useEffect, useRef, useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { DatasetTable, StudioDataset } from "../types";

export const SCHEMA_AUTOSAVE_DELAY_MS = 800;

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

function useDatasetCache(datasetId: string) {
    const queryClient = useQueryClient();
    return (dataset: StudioDataset) => {
        queryClient.setQueryData(studioDatasetQueryKey(datasetId), dataset);
        void queryClient.invalidateQueries({ queryKey: ["studio-datasets"] });
    };
}

export function useCreateDatasetDraft(datasetId: string) {
    const storeDataset = useDatasetCache(datasetId);
    return useMutation({
        mutationFn: () => studioApi.createDatasetDraft(datasetId),
        onSuccess: storeDataset,
    });
}

export function useDatasetSchemaAutosave(datasetId: string, onError: (error: Error) => void) {
    const storeDataset = useDatasetCache(datasetId);
    const [savedAt, setSavedAt] = useState<string | null>(null);
    const pending = useRef<DatasetTable[] | null>(null);
    const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
    const save = useMutation({
        mutationFn: (tables: DatasetTable[]) => studioApi.replaceDatasetSchema(datasetId, tables),
        onSuccess: (dataset: StudioDataset) => {
            setSavedAt(new Date().toISOString());
            storeDataset(dataset);
        },
        onError,
    });
    const saveRef = useRef(save.mutate);

    useEffect(() => {
        saveRef.current = save.mutate;
    }, [save.mutate]);

    useEffect(
        () => () => {
            clearTimeout(timer.current);
            if (pending.current) {
                saveRef.current(pending.current);
            }
        },
        [],
    );

    function queueSave(tables: DatasetTable[]) {
        pending.current = tables;
        clearTimeout(timer.current);
        timer.current = setTimeout(() => {
            const latest = pending.current;
            pending.current = null;
            if (latest) {
                saveRef.current(latest);
            }
        }, SCHEMA_AUTOSAVE_DELAY_MS);
    }

    function cancelSave() {
        clearTimeout(timer.current);
        pending.current = null;
    }

    return { queueSave, cancelSave, isSaving: save.isPending, error: save.error, savedAt };
}
