import { useEffect, useRef, useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { DatasetTable, StudioDataset, UpdateDatasetRequest } from "../types";

export const DATASET_AUTOSAVE_DELAY_MS = 800;

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

interface DebouncedSaveOptions<T> {
    datasetId: string;
    save: (value: T) => Promise<StudioDataset>;
    merge: (pending: T | null, change: T) => T;
    onError: (error: Error) => void;
}

function useDebouncedDatasetSave<T>({ datasetId, save, merge, onError }: DebouncedSaveOptions<T>) {
    const queryClient = useQueryClient();
    const [savedAt, setSavedAt] = useState<string | null>(null);
    const pending = useRef<T | null>(null);
    const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
    const mutation = useMutation({
        mutationFn: save,
        onSuccess: (dataset: StudioDataset) => {
            setSavedAt(new Date().toISOString());
            queryClient.setQueryData(studioDatasetQueryKey(datasetId), dataset);
            void queryClient.invalidateQueries({ queryKey: ["studio-datasets"] });
        },
        onError,
    });
    const saveRef = useRef(mutation.mutate);

    useEffect(() => {
        saveRef.current = mutation.mutate;
    }, [mutation.mutate]);

    useEffect(
        () => () => {
            clearTimeout(timer.current);
            if (pending.current !== null) {
                saveRef.current(pending.current);
            }
        },
        [],
    );

    function queueSave(change: T) {
        pending.current = merge(pending.current, change);
        clearTimeout(timer.current);
        timer.current = setTimeout(() => {
            const latest = pending.current;
            pending.current = null;
            if (latest !== null) {
                saveRef.current(latest);
            }
        }, DATASET_AUTOSAVE_DELAY_MS);
    }

    function cancelSave() {
        clearTimeout(timer.current);
        pending.current = null;
    }

    return { queueSave, cancelSave, isSaving: mutation.isPending, error: mutation.error, savedAt };
}

export function useDatasetSchemaAutosave(datasetId: string, onError: (error: Error) => void) {
    return useDebouncedDatasetSave<DatasetTable[]>({
        datasetId,
        save: (tables) => studioApi.replaceDatasetSchema(datasetId, tables),
        merge: (_, tables) => tables,
        onError,
    });
}

export function useDatasetInfoAutosave(datasetId: string, onError: (error: Error) => void) {
    return useDebouncedDatasetSave<UpdateDatasetRequest>({
        datasetId,
        save: (change) => studioApi.updateDataset(datasetId, change),
        merge: (pending, change) => ({ ...pending, ...change }),
        onError,
    });
}
