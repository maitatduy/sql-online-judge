import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

import { type MyProblemFilters, myProblemsQueryOf } from "./my-problem-filters";
import type { CreateProblemValues } from "./schemas";
import type {
    StatusCounts,
    StudioProblem,
    StudioProblemSummary,
    StudioTag,
    UpdateProblemRequest,
} from "./types";

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
};
