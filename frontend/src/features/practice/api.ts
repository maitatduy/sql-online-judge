import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

import { type ProblemFilters, problemsQueryOf } from "./problem-filters";
import type {
    CodeRun,
    ProblemDetail,
    ProblemSubmissionsResponse,
    ProblemSummary,
    RunAccepted,
    StartRunRequest,
    Submission,
    SubmissionAccepted,
    SubmitSolutionRequest,
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
    submitSolution: (problemId: string, request: SubmitSolutionRequest, idempotencyKey: string) =>
        apiClient.post<SubmissionAccepted>(
            `/api/problems/${encodeURIComponent(problemId)}/submissions`,
            request,
            { headers: { "Idempotency-Key": idempotencyKey } },
        ),
    getSubmission: (submissionId: string, signal?: AbortSignal) =>
        apiClient.get<Submission>(`/api/submissions/${encodeURIComponent(submissionId)}`, {
            signal,
        }),
    listProblemSubmissions: (problemId: string, cursor: string | undefined, signal?: AbortSignal) =>
        apiClient.get<ProblemSubmissionsResponse>(
            `/api/problems/${encodeURIComponent(problemId)}/submissions`,
            { params: { cursor }, signal },
        ),
};
