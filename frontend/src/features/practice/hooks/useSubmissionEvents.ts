import { useEffect, useRef, useState } from "react";

import { useQueryClient } from "@tanstack/react-query";

import { type ServerSentEvent, streamEvents } from "@/lib/sse";

import type { Submission } from "../types";

const RECONNECT_DELAY_MS = 2000;
const MAX_RECONNECT_ATTEMPTS = 3;

export function submissionQueryKey(submissionId: string | null) {
    return ["submission", submissionId] as const;
}

export function useSubmissionEvents(submissionId: string | null, onConnectionLost: () => void) {
    const queryClient = useQueryClient();
    const [failedSubmissionId, setFailedSubmissionId] = useState<string | null>(null);
    const onConnectionLostRef = useRef(onConnectionLost);

    useEffect(() => {
        onConnectionLostRef.current = onConnectionLost;
    }, [onConnectionLost]);

    useEffect(() => {
        if (submissionId === null) {
            return;
        }
        const controller = new AbortController();
        const queryKey = submissionQueryKey(submissionId);
        let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
        let reconnectAttempts = 0;
        let hasReportedLoss = false;

        function isCompleted(): boolean {
            return queryClient.getQueryData<Submission>(queryKey)?.status === "COMPLETED";
        }

        function receive(event: ServerSentEvent) {
            queryClient.setQueryData(queryKey, JSON.parse(event.data) as Submission);
            setFailedSubmissionId(null);
        }

        function handleStreamEnd() {
            if (controller.signal.aborted || isCompleted()) {
                return;
            }
            setFailedSubmissionId(submissionId);
            if (!hasReportedLoss) {
                hasReportedLoss = true;
                onConnectionLostRef.current();
            }
            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                reconnectAttempts += 1;
                reconnectTimer = setTimeout(connect, RECONNECT_DELAY_MS * reconnectAttempts);
            }
        }

        function connect() {
            if (isCompleted()) {
                return;
            }
            streamEvents(`/api/submissions/${encodeURIComponent(submissionId ?? "")}/events`, {
                signal: controller.signal,
                onEvent: receive,
            }).then(handleStreamEnd, handleStreamEnd);
        }

        connect();
        return () => {
            controller.abort();
            clearTimeout(reconnectTimer);
        };
    }, [submissionId, queryClient]);

    return { isPolling: submissionId !== null && failedSubmissionId === submissionId };
}
