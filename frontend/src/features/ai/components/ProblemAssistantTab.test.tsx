import { fireEvent, screen } from "@testing-library/react";
import { http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { resetAi } from "@/mocks/ai";
import { apiError } from "@/mocks/api-error";
import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ProblemAssistantTab } from "./ProblemAssistantTab";

vi.mock("next/navigation", () => ({
    usePathname: () => "/problems/count-orders-by-status",
}));

const SLUG = "count-orders-by-status";
const PROBLEM_ID = "0192f3b0-0000-7000-8000-000000000008";

function renderTab() {
    renderWithProviders(<ProblemAssistantTab problemSlug={SLUG} />);
}

describe("tab trợ lý AI trong trang làm bài", () => {
    beforeEach(() => {
        resetAi();
        localStorage.clear();
    });

    it("khách được mời đăng nhập", async () => {
        renderTab();

        expect(await screen.findByRole("link", { name: "Đăng nhập" })).toHaveAttribute(
            "href",
            `/login?next=%2Fproblems%2F${SLUG}`,
        );
    });

    it("hỏi kèm code đang viết, thấy câu trả lời và số lượt còn lại", async () => {
        writeMockSession(DEMO_SESSION_USER);
        localStorage.setItem(`draft:${PROBLEM_ID}:POSTGRESQL`, "SELECT * FROM orders");
        renderTab();

        fireEvent.change(await screen.findByLabelText("Câu hỏi cho trợ lý AI"), {
            target: { value: "Code mình sai ở đâu?" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Gửi" }));

        expect(await screen.findByText(/hãy gom các dòng theo cột cần nhóm/)).toBeVisible();
        expect(screen.getByText(/SELECT \* FROM orders/)).toBeVisible();
        expect(await screen.findByText("Còn 29/30 lượt hôm nay")).toBeVisible();
    });

    it("đang thi contest tính rating thì hiện trạng thái khoá thay cho ô hỏi", async () => {
        writeMockSession(DEMO_SESSION_USER);
        server.use(
            http.post("*/api/ai/conversations", () =>
                apiError(403, "AI_DISABLED_DURING_CONTEST", "Trợ lý AI tạm khoá"),
            ),
        );
        renderTab();

        fireEvent.click(await screen.findByRole("button", { name: "Gợi ý bước đầu tiên" }));

        expect(
            await screen.findByText("Trợ lý AI tạm khoá khi bạn đang thi contest tính rating."),
        ).toBeVisible();
        expect(screen.queryByLabelText("Câu hỏi cho trợ lý AI")).toBeNull();
    });

    it("hết lượt hỏi thì báo thời gian chờ", async () => {
        writeMockSession(DEMO_SESSION_USER);
        server.use(
            http.post("*/api/ai/conversations/:conversationId/messages", () =>
                apiError(429, "AI_QUOTA_EXCEEDED", "Hết lượt", {
                    headers: { "Retry-After": "7200" },
                }),
            ),
        );
        renderTab();

        fireEvent.click(await screen.findByRole("button", { name: "Bài này cần khái niệm gì?" }));

        expect(
            await screen.findByText("Bạn đã dùng hết lượt hỏi hôm nay. Thử lại sau khoảng 2 giờ."),
        ).toBeVisible();
    });
});
