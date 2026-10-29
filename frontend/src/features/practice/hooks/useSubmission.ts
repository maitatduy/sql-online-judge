import { useEffect, useState } from "react";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { useToast } from "@/components/shared/ToastProvider";

import { practiceApi } from "../api";
import type { SubmitSolutionRequest } from "../types";
import { problemSubmissionsQueryKey } from "./useProblemSubmissions";
import { submissionQueryKey, useSubmissionEvents } from "./useSubmissionEvents";

const SUBMISSION_POLL_INTERVAL_MS = 2000;
const CONNECTION_LOST_MESSAGE = "Mất kết nối máy chấm, đang thử lại…";

interface SubmitVariables {
    request: SubmitSolutionRequest;
    idempotencyKey: string;
}

export function useSubmission(problemId: string) {
    const showToast = useToast();
    const queryClient = useQueryClient();
    const [submissionId, setSubmissionId] = useState<string | null>(null);
    const start = useMutation({
        mutationFn: ({ request, idempotencyKey }: SubmitVariables) =>
            practiceApi.submitSolution(problemId, request, idempotencyKey),
        onMutate: () => setSubmissionId(null),
        onSuccess: (accepted) => setSubmissionId(accepted.submissionId),
    });
    const { isPolling } = useSubmissionEvents(submissionId, () =>
        showToast(CONNECTION_LOST_MESSAGE, "error"),
    );
    const submissionQuery = useQuery({
        queryKey: submissionQueryKey(submissionId),
        queryFn: ({ signal }) => practiceApi.getSubmission(submissionId ?? "", signal),
        enabled: isPolling,
        staleTime: Infinity,
        refetchInterval: (query) =>
            query.state.data?.status === "COMPLETED" ? false : SUBMISSION_POLL_INTERVAL_MS,
    });

    const submission = submissionId === null ? null : (submissionQuery.data ?? null);
    const isCompleted = submission?.status === "COMPLETED";

    useEffect(() => {
        if (isCompleted) {
            void queryClient.invalidateQueries({ queryKey: problemSubmissionsQueryKey(problemId) });
        }
    }, [isCompleted, problemId, queryClient]);

    const isJudging = start.isPending || (submissionId !== null && !isCompleted);

    function submit(request: SubmitSolutionRequest) {
        start.mutate({ request, idempotencyKey: crypto.randomUUID() });
    }

    return {
        submission,
        isJudging,
        error: start.error ?? (isPolling ? submissionQuery.error : null),
        submit,
    };
}
