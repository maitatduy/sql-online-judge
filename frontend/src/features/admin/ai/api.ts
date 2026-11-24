import { apiClient } from "@/lib/api-client";

import type { AiFlagPage, AiSummary, AiTranscript } from "./types";

export const AI_FLAG_PAGE_SIZE = 20;

export const adminAiApi = {
    summary: (signal?: AbortSignal) =>
        apiClient.get<AiSummary>("/api/admin/ai/summary", { signal }),
    flags: (page: number, signal?: AbortSignal) =>
        apiClient.get<AiFlagPage>("/api/admin/ai/flags", {
            params: { page, size: AI_FLAG_PAGE_SIZE },
            signal,
        }),
    transcript: (conversationId: string, signal?: AbortSignal) =>
        apiClient.get<AiTranscript>(
            `/api/admin/ai/conversations/${encodeURIComponent(conversationId)}`,
            { signal },
        ),
};
