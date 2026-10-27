import { useState } from "react";

import { useMutation, useQuery } from "@tanstack/react-query";

import { practiceApi } from "../api";
import type { SubmitSolutionRequest } from "../types";

const SUBMISSION_POLL_INTERVAL_MS = 2000;

interface SubmitVariables {
    request: SubmitSolutionRequest;
    idempotencyKey: string;
}

export function useSubmission(problemId: string) {
    const [submissionId, setSubmissionId] = useState<string | null>(null);
    const start = useMutation({
        mutationFn: ({ request, idempotencyKey }: SubmitVariables) =>
            practiceApi.submitSolution(problemId, request, idempotencyKey),
        onMutate: () => setSubmissionId(null),
        onSuccess: (accepted) => setSubmissionId(accepted.submissionId),
    });
    const submissionQuery = useQuery({
        queryKey: ["submission", submissionId],
        queryFn: ({ signal }) => practiceApi.getSubmission(submissionId ?? "", signal),
        enabled: submissionId !== null,
        staleTime: Infinity,
        refetchInterval: (query) =>
            query.state.data?.status === "COMPLETED" ? false : SUBMISSION_POLL_INTERVAL_MS,
    });

    const submission = submissionId === null ? null : (submissionQuery.data ?? null);
    const isJudging =
        start.isPending ||
        (submissionId !== null && submissionQuery.isPending) ||
        (submission !== null && submission.status !== "COMPLETED");

    function submit(request: SubmitSolutionRequest) {
        start.mutate({ request, idempotencyKey: crypto.randomUUID() });
    }

    return {
        submission,
        isJudging,
        error: start.error ?? (submissionId === null ? null : submissionQuery.error),
        submit,
    };
}
