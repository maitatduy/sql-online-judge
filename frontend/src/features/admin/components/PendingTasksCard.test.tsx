import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import type { PendingTask } from "../types";
import { PendingTasksCard } from "./PendingTasksCard";

function renderCard() {
    writeMockSession(ADMIN_SESSION_USER);
    return renderWithProviders(<PendingTasksCard />);
}

function respondWithTasks(items: PendingTask[]) {
    server.use(http.get("*/api/admin/pending-tasks", () => HttpResponse.json({ items })));
}

describe("PendingTasksCard", () => {
    it.each([
        ["Bài chờ duyệt", "/admin/problem-reviews", "5", "Cũ nhất gửi 3 ngày trước"],
        ["Báo cáo vi phạm", "/admin/reports", "12", "4 báo cáo về cùng một bình luận ở Weekly #37"],
        ["Hội thoại AI bị gắn cờ", "/admin/ai", "3", "Cố lấy đáp án đầy đủ trước khi AC"],
        ["Tin nhắn hỗ trợ chưa trả lời", "/admin/messages", "3", "Mới nhất lúc 15:20 từ hoa.kieu"],
    ])("dòng %s dẫn tới %s kèm số lượng và chi tiết", async (label, href, count, detail) => {
        renderCard();

        const link = await screen.findByRole("link", { name: new RegExp(`^${label}`) });

        expect(link).toHaveAttribute("href", href);
        expect(within(link).getByText(count)).toBeInTheDocument();
        expect(within(link).getByText(detail)).toBeInTheDocument();
    });

    it("có tiêu đề khối ngay cả khi đang tải", () => {
        renderCard();

        expect(screen.getByRole("heading", { level: 2, name: "Cần xử lý" })).toBeInTheDocument();
        expect(screen.getByRole("status", { name: "Đang tải việc cần xử lý" })).toBeVisible();
    });

    it("bỏ qua nhóm không còn việc nào", async () => {
        respondWithTasks([
            { type: "PROBLEM_REVIEW", count: 0, detail: "" },
            { type: "ABUSE_REPORT", count: 2, detail: "Mới nhất 5 phút trước" },
        ]);
        renderCard();

        expect(await screen.findByRole("link", { name: /^Báo cáo vi phạm/ })).toBeInTheDocument();
        expect(screen.getAllByRole("link")).toHaveLength(1);
    });

    it("báo trống khi không còn việc nào cần xử lý", async () => {
        respondWithTasks([{ type: "PROBLEM_REVIEW", count: 0, detail: "" }]);
        renderCard();

        expect(await screen.findByText("Không còn việc nào cần xử lý")).toBeInTheDocument();
        expect(screen.queryByRole("link")).not.toBeInTheDocument();
    });

    it("hiện số lượng lớn theo định dạng Việt Nam", async () => {
        respondWithTasks([{ type: "ABUSE_REPORT", count: 1250, detail: "Mới nhất 5 phút trước" }]);
        renderCard();

        const link = await screen.findByRole("link", { name: /^Báo cáo vi phạm/ });

        expect(within(link).getByText("1.250")).toBeInTheDocument();
    });

    it("hiện lỗi kèm mã lỗi và tải lại được", async () => {
        server.use(
            http.get(
                "*/api/admin/pending-tasks",
                () =>
                    HttpResponse.json(
                        {
                            status: 500,
                            code: "INTERNAL_ERROR",
                            message: "Đã có lỗi xảy ra",
                            traceId: "4bf92f3577b34da6",
                        },
                        { status: 500 },
                    ),
                { once: true },
            ),
        );
        renderCard();

        expect(await screen.findByRole("alert")).toHaveTextContent("4bf92f3577b34da6");

        fireEvent.click(screen.getByRole("button", { name: "Thử lại" }));

        expect(await screen.findByRole("link", { name: /^Bài chờ duyệt/ })).toBeInTheDocument();
    });
});
