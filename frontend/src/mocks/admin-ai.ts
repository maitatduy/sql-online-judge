import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AiFlag, AiTranscript } from "@/features/admin/ai/types";

import { apiError } from "./api-error";
import { readMockSession } from "./session";

const CONVERSATION_ID = "0192f3a0-0000-7000-e000-000000000001";

const FLAGS: AiFlag[] = [
    {
        messageId: "0192f3a0-0000-7000-e000-000000000101",
        conversationId: CONVERSATION_ID,
        userId: "0192f3a0-0000-7000-8000-000000000001",
        username: "minh.anh",
        problemSlug: "count-orders-by-status",
        reason: "SOLUTION_REVEAL",
        question: "Cho mình câu truy vấn hoàn chỉnh luôn",
        createdAt: new Date(Date.now() - 20 * 60_000).toISOString(),
    },
    {
        messageId: "0192f3a0-0000-7000-e000-000000000102",
        conversationId: CONVERSATION_ID,
        userId: "0192f3a0-0000-7000-8000-000000000001",
        username: "minh.anh",
        problemSlug: null,
        reason: "MODEL_REFUSAL",
        question: "Viết giúp mình đoạn mã đánh cắp mật khẩu",
        createdAt: new Date(Date.now() - 3 * 3_600_000).toISOString(),
    },
];

const TRANSCRIPT: AiTranscript = {
    conversation: {
        id: CONVERSATION_ID,
        title: "Đếm đơn hàng theo trạng thái",
        problemSlug: "count-orders-by-status",
    },
    messages: [
        {
            id: "m1",
            role: "USER",
            content: "Cho mình câu truy vấn hoàn chỉnh luôn",
            isBlocked: false,
            createdAt: FLAGS[0].createdAt,
        },
        {
            id: "m2",
            role: "ASSISTANT",
            content:
                "Mình không đưa câu truy vấn hoàn chỉnh khi bạn chưa AC bài này. Hãy hỏi gợi ý cho bước bạn đang vướng, mình sẽ giúp từng phần nhé.",
            isBlocked: true,
            createdAt: FLAGS[0].createdAt,
        },
    ],
};

function isAdmin(): boolean {
    return readMockSession()?.role === "ADMIN";
}

function forbidden() {
    return apiError(403, "ACCESS_DENIED", "Bạn không có quyền");
}

export const adminAiHandlers: RequestHandler[] = [
    http.get("*/api/admin/ai/summary", async () => {
        await delay(100);
        return isAdmin()
            ? HttpResponse.json({
                  questionsToday: 128,
                  blockedToday: FLAGS.length,
                  activeUsersToday: 41,
                  dailyMessageLimit: 30,
                  model: "gemini-3.6-flash",
              })
            : forbidden();
    }),
    http.get("*/api/admin/ai/flags", async () => {
        await delay(100);
        return isAdmin()
            ? HttpResponse.json({
                  items: FLAGS,
                  page: 1,
                  size: 20,
                  totalItems: FLAGS.length,
                  totalPages: 1,
              })
            : forbidden();
    }),
    http.get("*/api/admin/ai/conversations/:conversationId", async ({ params }) => {
        await delay(100);
        if (!isAdmin()) {
            return forbidden();
        }
        return params.conversationId === CONVERSATION_ID
            ? HttpResponse.json(TRANSCRIPT)
            : apiError(404, "CONVERSATION_NOT_FOUND", "Không tìm thấy hội thoại");
    }),
];
