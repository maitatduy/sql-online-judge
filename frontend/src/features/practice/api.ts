import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

import { type ProblemFilters, problemsQueryOf } from "./problem-filters";
import type {
    CodeRun,
    ProblemDetail,
    ProblemSummary,
    RunAccepted,
    StartRunRequest,
    TagsResponse,
} from "./types";

export const practiceApi = {
    listProblems: (filters: ProblemFilters, signal?: AbortSignal) =>
        apiClient.get<Page<ProblemSummary>>("/api/problems", {
            params: problemsQueryOf(filters),
            signal,
        }),
    listTags: (signal?: AbortSignal) => apiClient.get<TagsResponse>("/api/tags", { signal }),
    getProblem: (slug: string, signal?: AbortSignal) =>
        apiClient.get<ProblemDetail>(`/api/problems/${encodeURIComponent(slug)}`, { signal }),
    startRun: (problemId: string, request: StartRunRequest) =>
        apiClient.post<RunAccepted>(`/api/problems/${encodeURIComponent(problemId)}/runs`, request),
    getRun: (runId: string, signal?: AbortSignal) =>
        apiClient.get<CodeRun>(`/api/runs/${encodeURIComponent(runId)}`, { signal }),
};
