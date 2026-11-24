import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminAi } from "./AdminAi";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

describe("màn quản trị trợ lý AI", () => {
    beforeEach(() => {
        visit("/admin/ai");
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("hiện số liệu hôm nay và các câu trả lời bị chặn kèm lý do", async () => {
        renderWithProviders(<AdminAi />);

        const questions = await screen.findByRole("group", { name: "Câu hỏi hôm nay" });
        expect(questions).toHaveTextContent("128");
        expect(screen.getByRole("group", { name: "Lượt mỗi người mỗi ngày" })).toHaveTextContent(
            "gemini-3.6-flash",
        );
        const rows = within(screen.getByRole("table")).getAllByRole("row");
        expect(rows).toHaveLength(3);
        expect(rows[1]).toHaveTextContent("Lộ lời giải");
        expect(rows[1]).toHaveTextContent("Cho mình câu truy vấn hoàn chỉnh luôn");
        expect(rows[2]).toHaveTextContent("Mô hình từ chối");
    });

    it("mở hội thoại để xem toàn bộ tin nhắn, câu bị chặn được đánh dấu", async () => {
        renderWithProviders(<AdminAi />);

        fireEvent.click((await screen.findAllByRole("button", { name: "Xem hội thoại" }))[0]);

        const dialog = await screen.findByRole("dialog");
        expect(await within(dialog).findByText("Trợ lý - bị chặn")).toBeVisible();
        expect(within(dialog).getByText("Đếm đơn hàng theo trạng thái")).toBeVisible();
    });
});
