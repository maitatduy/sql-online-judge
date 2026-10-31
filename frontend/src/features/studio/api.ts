import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

import { type MyProblemFilters, myProblemsQueryOf } from "./my-problem-filters";
import type { CreateProblemValues } from "./schemas";
import type { StatusCounts, StudioProblemSummary } from "./types";

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
};
