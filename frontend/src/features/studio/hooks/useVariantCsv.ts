import { useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiRequestError } from "@/lib/api-client";

import { studioApi } from "../api";
import { rowsToCsv } from "../csv";
import type { VariantCsv } from "../types";
import type { VariantTableDraft } from "../variant-drafts";
import { studioDatasetQueryKey } from "./useStudioDataset";

interface VariantTableRef {
    datasetId: string;
    variantId: string;
    tableName: string;
}

export class VariantTableSaveError extends Error {
    readonly draft: VariantTableDraft;

    constructor(draft: VariantTableDraft, error: Error) {
        super(
            error instanceof ApiRequestError && error.errors.length > 0
                ? error.errors[0].message
                : error.message,
        );
        this.draft = draft;
    }
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

export function useSaveVariantDrafts(
    datasetId: string,
    onDraftSaved: (draft: VariantTableDraft) => void,
) {
    const queryClient = useQueryClient();
    const [savedAt, setSavedAt] = useState<string | null>(null);

    async function saveDraft(draft: VariantTableDraft) {
        const csv = rowsToCsv(draft.columns, draft.rows);
        const saved = await studioApi
            .saveVariantCsv(datasetId, draft.variantId, draft.tableName, csv)
            .catch((error: Error) => {
                throw new VariantTableSaveError(draft, error);
            });
        const ref = { datasetId, variantId: draft.variantId, tableName: draft.tableName };
        queryClient.setQueryData<VariantCsv>(variantCsvQueryKey(ref), {
            tableName: draft.tableName,
            csv,
            rowCount: draft.rows.length,
        });
        queryClient.setQueryData(studioDatasetQueryKey(datasetId), saved);
        onDraftSaved(draft);
    }

    const mutation = useMutation({
        mutationFn: async (drafts: VariantTableDraft[]) => {
            for (const draft of drafts) {
                await saveDraft(draft);
            }
        },
        onSuccess: () => setSavedAt(new Date().toISOString()),
    });

    return {
        save: mutation.mutate,
        isSaving: mutation.isPending,
        error: mutation.error,
        savedAt,
    };
}
