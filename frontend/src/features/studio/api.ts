import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

import { type MyDatasetFilters, myDatasetsQueryOf } from "./my-dataset-filters";
import { type MyProblemFilters, myProblemsQueryOf } from "./my-problem-filters";
import type { CreateDatasetValues, CreateProblemValues } from "./schemas";
import type {
    DatasetTable,
    StudioDataset,
    StudioDatasetSummary,
    StatusCounts,
    StudioProblem,
    StudioProblemSummary,
    StudioTag,
    UpdateProblemRequest,
} from "./types";

function datasetPath(datasetId: string): string {
    return `/api/studio/datasets/${encodeURIComponent(datasetId)}`;
}

export const studioApi = {
    listMyProblems: (filters: MyProblemFilters, signal?: AbortSignal) =>
        apiClient.get<Page<StudioProblemSummary>>("/api/studio/problems", {
            params: myProblemsQueryOf(filters),
            signal,
        }),
    getStatusCounts: (signal?: AbortSignal) =>
        apiClient.get<StatusCounts>("/api/studio/problems/status-counts", { signal }),
    createProblem: (values: CreateProblemValues) =>
        apiClient.post<StudioProblemSummary>("/api/studio/problems", values),
    getProblem: (problemId: string, signal?: AbortSignal) =>
        apiClient.get<StudioProblem>(`/api/studio/problems/${encodeURIComponent(problemId)}`, {
            signal,
        }),
    updateProblem: (problemId: string, request: UpdateProblemRequest) =>
        apiClient.patch<StudioProblem>(
            `/api/studio/problems/${encodeURIComponent(problemId)}`,
            request,
        ),
    listTags: (signal?: AbortSignal) =>
        apiClient.get<{ items: StudioTag[] }>("/api/tags", { signal }),
    listMyDatasets: (filters: MyDatasetFilters, signal?: AbortSignal) =>
        apiClient.get<Page<StudioDatasetSummary>>("/api/studio/datasets", {
            params: myDatasetsQueryOf(filters),
            signal,
        }),
    createDataset: (values: CreateDatasetValues) =>
        apiClient.post<StudioDataset>("/api/studio/datasets", values),
    getDataset: (datasetId: string, signal?: AbortSignal) =>
        apiClient.get<StudioDataset>(datasetPath(datasetId), { signal }),
    replaceDatasetSchema: (datasetId: string, tables: DatasetTable[]) =>
        apiClient.put<StudioDataset>(`${datasetPath(datasetId)}/schema`, { tables }),
    createDatasetDraft: (datasetId: string) =>
        apiClient.post<StudioDataset>(`${datasetPath(datasetId)}/versions`),
};
