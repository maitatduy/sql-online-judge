import type { Page } from "@/lib/pagination";

export type MessageRole = "USER" | "ASSISTANT";

export interface AiQuota {
    used: number;
    limit: number;
    resetsAt: string;
}

export interface ConversationSummary {
    id: string;
    title: string;
    problemId: string | null;
    problemSlug: string | null;
    lastMessageAt: string;
}

export type ConversationPage = Page<ConversationSummary>;

export interface AiMessage {
    id: string;
    role: MessageRole;
    content: string;
    isBlocked: boolean;
    createdAt: string;
}

export interface ConversationDetail {
    conversation: ConversationSummary;
    messages: AiMessage[];
}

export interface MessageExchange {
    question: AiMessage;
    answer: AiMessage;
    quota: AiQuota;
}

export interface AssistantProblem {
    id: string;
    slug: string;
    title: string;
}
