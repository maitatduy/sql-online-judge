import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

import { type MyDatasetFilters, myDatasetsQueryOf } from "./my-dataset-filters";
import { type MyProblemFilters, myProblemsQueryOf } from "./my-problem-filters";
import type { CreateDatasetValues, CreateProblemValues } from "./schemas";
import type {
    CreateStudioRunRequest,
    DatasetTable,
    DatasetVariantRequest,
    SqlImportPreview,
    StudioDataset,
    StudioDatasetSummary,
    StudioProblem,
    StudioProblemSummary,
    StudioRun,
    StudioTag,
    UpdateDatasetRequest,
    UpdateProblemRequest,
    VariantCsv,
} from "./types";

function datasetPath(datasetId: string): string {
    return `/api/studio/datasets/${encodeURIComponent(datasetId)}`;
}

function variantTablePath(datasetId: string, variantId: string, tableName: string): string {
    return `${datasetPath(datasetId)}/variants/${encodeURIComponent(variantId)}/tables/${encodeURIComponent(tableName)}`;
}

export const studioApi = {
    listMyProblems: (filters: MyProblemFilters, signal?: AbortSignal) =>
        apiClient.get<Page<StudioProblemSummary>>("/api/studio/problems", {
            params: myProblemsQueryOf(filters),
            signal,
        }),
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
    chooseProblemDataset: (problemId: string, datasetId: string) =>
        apiClient.put<StudioProblem>(
            `/api/studio/problems/${encodeURIComponent(problemId)}/dataset`,
            { datasetId },
        ),
    createStudioRun: (problemId: string, request: CreateStudioRunRequest) =>
        apiClient.post<StudioRun>(
            `/api/studio/problems/${encodeURIComponent(problemId)}/runs`,
            request,
        ),
    getStudioRun: (problemId: string, runId: string, signal?: AbortSignal) =>
        apiClient.get<StudioRun>(
            `/api/studio/problems/${encodeURIComponent(problemId)}/runs/${encodeURIComponent(runId)}`,
            { signal },
        ),
    deleteProblem: (problemId: string) =>
        apiClient.delete<void>(`/api/studio/problems/${encodeURIComponent(problemId)}`),
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
    parseSql: (sql: string) =>
        apiClient.post<SqlImportPreview>("/api/studio/datasets/parse-sql", { sql }),
    deleteDataset: (datasetId: string) => apiClient.delete<void>(datasetPath(datasetId)),
    addDatasetVariant: (datasetId: string, variant: DatasetVariantRequest) =>
        apiClient.post<StudioDataset>(`${datasetPath(datasetId)}/variants`, variant),
    updateDatasetVariant: (datasetId: string, variantId: string, variant: DatasetVariantRequest) =>
        apiClient.patch<StudioDataset>(
            `${datasetPath(datasetId)}/variants/${encodeURIComponent(variantId)}`,
            variant,
        ),
    getVariantCsv: (
        datasetId: string,
        variantId: string,
        tableName: string,
        signal?: AbortSignal,
    ) => apiClient.get<VariantCsv>(variantTablePath(datasetId, variantId, tableName), { signal }),
    saveVariantCsv: (datasetId: string, variantId: string, tableName: string, csv: string) =>
        apiClient.put<StudioDataset>(variantTablePath(datasetId, variantId, tableName), { csv }),
    deleteDatasetVariant: (datasetId: string, variantId: string) =>
        apiClient.delete<void>(
            `${datasetPath(datasetId)}/variants/${encodeURIComponent(variantId)}`,
        ),
    updateDataset: (datasetId: string, change: UpdateDatasetRequest) =>
        apiClient.patch<StudioDataset>(datasetPath(datasetId), change),
};
