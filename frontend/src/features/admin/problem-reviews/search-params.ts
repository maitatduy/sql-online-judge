import type { ReviewProblemStatus } from "./types";

export const REVIEW_STATUSES: readonly ReviewProblemStatus[] = [
    "PENDING_REVIEW",
    "CHANGES_REQUESTED",
    "REJECTED",
    "PUBLISHED",
];

export interface ReviewSelection {
    status: ReviewProblemStatus;
    problemId: string | null;
}

export function readReviewSelection(params: URLSearchParams): ReviewSelection {
    const status = REVIEW_STATUSES.find((candidate) => candidate === params.get("status"));
    return { status: status ?? "PENDING_REVIEW", problemId: params.get("problem") };
}

export function reviewHref({ status, problemId }: ReviewSelection): string {
    const params = new URLSearchParams();
    if (status !== "PENDING_REVIEW") {
        params.set("status", status);
    }
    if (problemId) {
        params.set("problem", problemId);
    }
    const query = params.toString();
    return `/admin/problem-reviews${query ? `?${query}` : ""}`;
}
