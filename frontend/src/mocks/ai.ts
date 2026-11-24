import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AiMessage, ConversationSummary } from "@/features/ai/types";

import { apiError } from "./api-error";
import { findProblem } from "./practice-sql";
import { readMockSession } from "./session";

const DAILY_LIMIT = 30;
const MOCK_REPLY =
    "Gợi ý: hãy gom các dòng theo cột cần nhóm bằng `GROUP BY`, rồi dùng hàm tổng hợp như `COUNT(*)`.";

interface MockConversation extends ConversationSummary {
    ownerId: string;
    messages: AiMessage[];
}

let conversations: MockConversation[] = [];
let used = 0;

export function resetAi() {
    conversations = [];
    used = 0;
}

function quota() {
    const resetsAt = new Date();
    resetsAt.setUTCHours(17, 0, 0, 0);
    return { used, limit: DAILY_LIMIT, resetsAt: resetsAt.toISOString() };
}

function message(role: AiMessage["role"], content: string): AiMessage {
    return {
        id: crypto.randomUUID(),
        role,
        content,
        isBlocked: false,
        createdAt: new Date().toISOString(),
    };
}

function summaryOf(conversation: MockConversation): ConversationSummary {
    return {
        id: conversation.id,
        title: conversation.title,
        problemId: conversation.problemId,
        problemSlug: conversation.problemSlug,
        lastMessageAt: conversation.lastMessageAt,
    };
}

function owned(conversationId: unknown): MockConversation | undefined {
    const userId = readMockSession()?.id;
    return conversations.find(
        (conversation) => conversation.id === conversationId && conversation.ownerId === userId,
    );
}

function unauthenticated() {
    return apiError(401, "UNAUTHORIZED", "Bạn cần đăng nhập");
}

export const aiHandlers: RequestHandler[] = [
    http.get("*/api/ai/quota", () =>
        readMockSession() ? HttpResponse.json(quota()) : unauthenticated(),
    ),
    http.get("*/api/ai/conversations", async () => {
        await delay(100);
        const userId = readMockSession()?.id;
        if (!userId) {
            return unauthenticated();
        }
        const items = conversations
            .filter((conversation) => conversation.ownerId === userId)
            .sort((a, b) => b.lastMessageAt.localeCompare(a.lastMessageAt))
            .map(summaryOf);
        return HttpResponse.json({
            items,
            page: 1,
            size: 30,
            totalItems: items.length,
            totalPages: 1,
        });
    }),
    http.get("*/api/ai/conversations/:conversationId", async ({ params }) => {
        await delay(100);
        const conversation = owned(params.conversationId);
        return conversation
            ? HttpResponse.json({
                  conversation: summaryOf(conversation),
                  messages: conversation.messages,
              })
            : apiError(404, "CONVERSATION_NOT_FOUND", "Không tìm thấy hội thoại");
    }),
    http.post("*/api/ai/conversations", async ({ request }) => {
        const userId = readMockSession()?.id;
        if (!userId) {
            return unauthenticated();
        }
        const { problemId } = (await request.json()) as { problemId: string | null };
        const problem = problemId ? findProblem(problemId) : null;
        const conversation: MockConversation = {
            id: crypto.randomUUID(),
            ownerId: userId,
            title: problem?.title ?? "Hội thoại mới",
            problemId: problem?.id ?? null,
            problemSlug: problem?.slug ?? null,
            lastMessageAt: new Date().toISOString(),
            messages: [],
        };
        conversations.push(conversation);
        return HttpResponse.json(summaryOf(conversation), { status: 201 });
    }),
    http.post("*/api/ai/conversations/:conversationId/messages", async ({ params, request }) => {
        await delay(300);
        const conversation = owned(params.conversationId);
        if (!conversation) {
            return apiError(404, "CONVERSATION_NOT_FOUND", "Không tìm thấy hội thoại");
        }
        if (used >= DAILY_LIMIT) {
            return apiError(429, "AI_QUOTA_EXCEEDED", "Bạn đã dùng hết lượt hỏi AI hôm nay", {
                headers: { "Retry-After": "3600" },
            });
        }
        const { content, code } = (await request.json()) as { content: string; code?: string };
        used += 1;
        const question = message(
            "USER",
            code ? `${content}\n\nCode hiện tại của tôi:\n\`\`\`sql\n${code}\n\`\`\`` : content,
        );
        const answer = message("ASSISTANT", MOCK_REPLY);
        if (conversation.messages.length === 0 && !conversation.problemId) {
            conversation.title = content.slice(0, 200);
        }
        conversation.messages.push(question, answer);
        conversation.lastMessageAt = answer.createdAt;
        return HttpResponse.json({ question, answer, quota: quota() });
    }),
];
