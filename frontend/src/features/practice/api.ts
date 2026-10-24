import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

import { type ProblemFilters, problemsQueryOf } from "./problem-filters";
import type { ProblemSummary, TopicsResponse } from "./types";

export const practiceApi = {
    listProblems: (filters: ProblemFilters, signal?: AbortSignal) =>
        apiClient.get<Page<ProblemSummary>>("/api/problems", {
            params: problemsQueryOf(filters),
            signal,
        }),
    listTopics: (signal?: AbortSignal) =>
        apiClient.get<TopicsResponse>("/api/problems/topics", { signal }),
};
