import { apiClient } from "@/lib/api-client";

import type {
    AiQuota,
    AssistantProblem,
    ConversationDetail,
    ConversationPage,
    ConversationSummary,
    MessageExchange,
} from "./types";

const CONVERSATION_PAGE_SIZE = 30;

function conversationPath(conversationId: string): string {
    return `/api/ai/conversations/${encodeURIComponent(conversationId)}`;
}

export interface NewQuestion {
    conversationId: string;
    content: string;
    code?: string;
}

export const aiApi = {
    quota: (signal?: AbortSignal) => apiClient.get<AiQuota>("/api/ai/quota", { signal }),
    conversations: (signal?: AbortSignal) =>
        apiClient.get<ConversationPage>("/api/ai/conversations", {
            params: { page: 1, size: CONVERSATION_PAGE_SIZE },
            signal,
        }),
    conversation: (conversationId: string, signal?: AbortSignal) =>
        apiClient.get<ConversationDetail>(conversationPath(conversationId), { signal }),
    start: (problemId: string | null) =>
        apiClient.post<ConversationSummary>("/api/ai/conversations", { problemId }),
    ask: ({ conversationId, content, code }: NewQuestion) =>
        apiClient.post<MessageExchange>(`${conversationPath(conversationId)}/messages`, {
            content,
            code,
        }),
    problem: (slug: string, signal?: AbortSignal) =>
        apiClient.get<AssistantProblem>(`/api/problems/${encodeURIComponent(slug)}`, { signal }),
};
