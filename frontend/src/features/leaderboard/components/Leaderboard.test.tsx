import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { clearMockSession, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { Leaderboard } from "./Leaderboard";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function renderRows() {
    renderWithProviders(<Leaderboard />);
    return within(await screen.findByRole("table")).getAllByRole("row");
}

describe("bảng xếp hạng", () => {
    beforeEach(() => visit("/leaderboard"));
    afterEach(() => clearMockSession());

    it("có breadcrumb Trang chủ dẫn về trang chủ", async () => {
        renderWithProviders(<Leaderboard />);

        const breadcrumb = within(await screen.findByRole("navigation", { name: "Đường dẫn" }));
        expect(breadcrumb.getByRole("link", { name: "Trang chủ" })).toHaveAttribute("href", "/");
        expect(breadcrumb.getByText("Bảng xếp hạng")).toHaveAttribute("aria-current", "page");
    });

    it("liệt kê người thi theo hạng, kèm tên hiển thị dẫn tới hồ sơ và tên người dùng", async () => {
        const rows = await renderRows();

        expect(within(rows[0]).getByRole("columnheader", { name: "Tên người dùng" })).toBeVisible();
        expect(rows[1]).toHaveTextContent("1.820");
        expect(within(rows[1]).getByRole("link", { name: "Bùi An" })).toHaveAttribute(
            "href",
            "/u/an.bui",
        );
        expect(within(rows[1]).getByText("@an.bui")).toBeVisible();
    });

    it("chia trang 20 người, trang 2 bắt đầu từ hạng 21", async () => {
        visit("/leaderboard?page=2");
        const rows = await renderRows();

        expect(rows[1]).toHaveTextContent(/^21/);
        expect(screen.getByRole("link", { name: "Trang 2" })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(screen.getByRole("link", { name: "Trang 1" })).toHaveAttribute(
            "href",
            "/leaderboard",
        );
    });

    it("tô nổi bật dòng của người đang đăng nhập", async () => {
        writeMockSession(DEMO_SESSION_USER);
        const rows = await renderRows();

        const mine = await within(screen.getByRole("table")).findByText("Bạn");
        expect(mine.closest("tr")).toHaveTextContent("Nguyễn Minh Anh");
        expect(mine.closest("tr")).toHaveAttribute("aria-current", "true");
        expect(rows.filter((row) => row.getAttribute("aria-current") === "true")).toHaveLength(1);
    });

    it("chưa có ai thi thì hiện trạng thái trống", async () => {
        server.use(
            http.get("*/api/leaderboard", () =>
                HttpResponse.json({ items: [], page: 1, size: 20, totalItems: 0, totalPages: 1 }),
            ),
        );
        renderWithProviders(<Leaderboard />);

        expect(await screen.findByText("Chưa có ai thi contest tính rating.")).toBeVisible();
    });
});
