import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { adminAiApi } from "../ai/api";

export function useAiSummary() {
    return useQuery({
        queryKey: ["admin", "ai", "summary"],
        queryFn: ({ signal }) => adminAiApi.summary(signal),
        retry: false,
    });
}

export function useAiFlags(page: number) {
    return useQuery({
        queryKey: ["admin", "ai", "flags", page],
        queryFn: ({ signal }) => adminAiApi.flags(page, signal),
        placeholderData: keepPreviousData,
        retry: false,
    });
}

export function useAiTranscript(conversationId: string) {
    return useQuery({
        queryKey: ["admin", "ai", "transcript", conversationId],
        queryFn: ({ signal }) => adminAiApi.transcript(conversationId, signal),
        retry: false,
    });
}
