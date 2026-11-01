import { useMutation, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { DatasetVariantRequest, StudioDataset } from "../types";
import { studioDatasetQueryKey } from "./useStudioDataset";

export function useDatasetVariants(datasetId: string) {
    const queryClient = useQueryClient();
    const storeDataset = (dataset: StudioDataset) =>
        queryClient.setQueryData(studioDatasetQueryKey(datasetId), dataset);

    const addVariant = useMutation({
        mutationFn: (variant: DatasetVariantRequest) =>
            studioApi.addDatasetVariant(datasetId, variant),
        onSuccess: storeDataset,
    });
    const updateVariant = useMutation({
        mutationFn: ({
            variantId,
            variant,
        }: {
            variantId: string;
            variant: DatasetVariantRequest;
        }) => studioApi.updateDatasetVariant(datasetId, variantId, variant),
        onSuccess: storeDataset,
    });
    const deleteVariant = useMutation({
        mutationFn: (variantId: string) => studioApi.deleteDatasetVariant(datasetId, variantId),
        onSuccess: () =>
            queryClient.invalidateQueries({ queryKey: studioDatasetQueryKey(datasetId) }),
    });

    return { addVariant, updateVariant, deleteVariant };
}
