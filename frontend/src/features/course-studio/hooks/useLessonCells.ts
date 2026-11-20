import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { courseStudioApi } from "../api";
import type { CellRequest } from "../types";
import { STUDIO_COURSES_KEY } from "./useMyCourses";

function cellsKey(lessonId: string) {
    return [...STUDIO_COURSES_KEY, "cells", lessonId] as const;
}

export function useLessonCells(lessonId: string) {
    return useQuery({
        queryKey: cellsKey(lessonId),
        queryFn: ({ signal }) => courseStudioApi.listCells(lessonId, signal),
    });
}

function useCellMutation<TVariables, TResult>(
    lessonId: string,
    mutationFn: (variables: TVariables) => Promise<TResult>,
) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: cellsKey(lessonId) }),
    });
}

export function useAddCell(lessonId: string) {
    return useCellMutation(lessonId, (request: CellRequest) =>
        courseStudioApi.addCell(lessonId, request),
    );
}

export function useUpdateCell(lessonId: string) {
    return useCellMutation(
        lessonId,
        ({ cellId, request }: { cellId: string; request: CellRequest }) =>
            courseStudioApi.updateCell(cellId, request),
    );
}

export function useDeleteCell(lessonId: string) {
    return useCellMutation(lessonId, (cellId: string) => courseStudioApi.deleteCell(cellId));
}

export function useDatasetOptions(enabled: boolean) {
    return useQuery({
        queryKey: [...STUDIO_COURSES_KEY, "datasets"],
        queryFn: ({ signal }) => courseStudioApi.listDatasets(signal),
        enabled,
    });
}

export function useDatasetChoice(datasetId: string) {
    return useQuery({
        queryKey: [...STUDIO_COURSES_KEY, "dataset", datasetId],
        queryFn: ({ signal }) => courseStudioApi.getDataset(datasetId, signal),
        enabled: datasetId !== "",
    });
}
