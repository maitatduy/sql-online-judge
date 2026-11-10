import { apiClient } from "@/lib/api-client";

import type {
    ReviewAction,
    ReviewAnswerCheck,
    ReviewProblemDetail,
    ReviewProblemPage,
    ReviewProblemStatus,
} from "./types";

export const REVIEW_QUEUE_PAGE_SIZE = 50;

function problemPath(problemId: string): string {
    return encodeURIComponent(problemId);
}

export const problemReviewApi = {
    getQueue: (status: ReviewProblemStatus, signal?: AbortSignal) =>
        apiClient.get<ReviewProblemPage>("/api/admin/problems", {
            params: { status, page: 1, size: REVIEW_QUEUE_PAGE_SIZE },
            signal,
        }),
    getProblem: (problemId: string, signal?: AbortSignal) =>
        apiClient.get<ReviewProblemDetail>(`/api/studio/problems/${problemPath(problemId)}`, {
            signal,
        }),
    getLatestAnswerCheck: (problemId: string, signal?: AbortSignal) =>
        apiClient.get<ReviewAnswerCheck>(
            `/api/studio/problems/${problemPath(problemId)}/answer-checks/latest`,
            { signal },
        ),
    review: (problemId: string, action: ReviewAction, reason: string | null) =>
        apiClient.post<ReviewProblemDetail>(
            `/api/admin/problems/${problemPath(problemId)}/${action}`,
            reason === null ? undefined : { reason },
        ),
};
