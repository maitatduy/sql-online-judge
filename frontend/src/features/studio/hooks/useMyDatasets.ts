import { useSearchParams } from "next/navigation";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import { type MyDatasetFilters, myDatasetsHref, readMyDatasetFilters } from "../my-dataset-filters";

function changeFilters(change: Partial<Omit<MyDatasetFilters, "page">>): void {
    const current = readMyDatasetFilters(new URLSearchParams(window.location.search));
    window.history.replaceState(null, "", myDatasetsHref({ ...current, ...change, page: 1 }));
}

export function useMyDatasetFilters() {
    return { filters: readMyDatasetFilters(useSearchParams()), changeFilters };
}

export function useMyDatasets(filters: MyDatasetFilters) {
    return useQuery({
        queryKey: ["studio-datasets", "list", filters],
        queryFn: ({ signal }) => studioApi.listMyDatasets(filters, signal),
        placeholderData: keepPreviousData,
    });
}

export function useCreateDataset() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: studioApi.createDataset,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["studio-datasets"] }),
    });
}
