import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiRequestError } from "@/lib/api-client";

import { problemReviewApi } from "../problem-reviews/api";
import type {
    ReviewAction,
    ReviewAnswerCheck,
    ReviewProblemDetail,
    ReviewProblemStatus,
} from "../problem-reviews/types";

const REVIEW_QUEUE_KEY = "admin-problem-reviews";

export function useReviewQueue(status: ReviewProblemStatus) {
    return useQuery({
        queryKey: [REVIEW_QUEUE_KEY, status],
        queryFn: ({ signal }) => problemReviewApi.getQueue(status, signal),
    });
}

export function useReviewProblem(problemId: string) {
    return useQuery({
        queryKey: ["admin-problem-review", problemId],
        queryFn: ({ signal }) => problemReviewApi.getProblem(problemId, signal),
        retry: false,
    });
}

export function useReviewAnswerCheck(problemId: string) {
    return useQuery({
        queryKey: ["admin-problem-review", problemId, "answer-check"],
        queryFn: async ({ signal }): Promise<ReviewAnswerCheck | null> => {
            try {
                return await problemReviewApi.getLatestAnswerCheck(problemId, signal);
            } catch (error) {
                if (error instanceof ApiRequestError && error.status === 404) {
                    return null;
                }
                throw error;
            }
        },
        retry: false,
    });
}

interface ReviewRequest {
    action: ReviewAction;
    reason: string | null;
}

export function useReviewDecision(
    problemId: string,
    onReviewed: (problem: ReviewProblemDetail, action: ReviewAction) => void,
) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ action, reason }: ReviewRequest) =>
            problemReviewApi.review(problemId, action, reason),
        onSuccess: (problem, { action }) => {
            queryClient.setQueryData(["admin-problem-review", problemId], problem);
            void queryClient.invalidateQueries({ queryKey: [REVIEW_QUEUE_KEY] });
            onReviewed(problem, action);
        },
    });
}
