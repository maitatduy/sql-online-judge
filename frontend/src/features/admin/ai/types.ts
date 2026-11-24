import type { Page } from "@/lib/pagination";

export type AiBlockReason = "SOLUTION_REVEAL" | "MODEL_REFUSAL";

export interface AiSummary {
    questionsToday: number;
    blockedToday: number;
    activeUsersToday: number;
    dailyMessageLimit: number;
    model: string;
}

export interface AiFlag {
    messageId: string;
    conversationId: string;
    userId: string;
    username: string | null;
    problemSlug: string | null;
    reason: AiBlockReason;
    question: string | null;
    createdAt: string;
}

export type AiFlagPage = Page<AiFlag>;

export interface AiTranscriptMessage {
    id: string;
    role: "USER" | "ASSISTANT";
    content: string;
    isBlocked: boolean;
    createdAt: string;
}

export interface AiTranscript {
    conversation: { id: string; title: string; problemSlug: string | null };
    messages: AiTranscriptMessage[];
}
