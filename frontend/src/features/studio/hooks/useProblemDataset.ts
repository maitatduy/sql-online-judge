import { useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { MyDatasetFilters } from "../my-dataset-filters";
import type { StudioProblem } from "../types";
import { studioProblemQueryKey } from "./useStudioProblem";

const DATASET_OPTIONS: MyDatasetFilters = { q: "", sort: "name,asc", size: 100, page: 1 };

export function useDatasetOptions() {
    return useQuery({
        queryKey: ["studio-datasets", "list", DATASET_OPTIONS],
        queryFn: ({ signal }) => studioApi.listMyDatasets(DATASET_OPTIONS, signal),
    });
}

export function useChooseDataset(problemId: string, onError: (error: Error) => void) {
    const queryClient = useQueryClient();
    const [savedAt, setSavedAt] = useState<string | null>(null);
    const mutation = useMutation({
        mutationFn: (datasetId: string) => studioApi.chooseProblemDataset(problemId, datasetId),
        onSuccess: (problem: StudioProblem) => {
            setSavedAt(new Date().toISOString());
            queryClient.setQueryData(studioProblemQueryKey(problemId), problem);
            void queryClient.invalidateQueries({ queryKey: ["studio-problems"] });
        },
        onError,
    });

    return {
        choose: mutation.mutate,
        reset: mutation.reset,
        isSaving: mutation.isPending,
        error: mutation.error,
        savedAt,
    };
}
