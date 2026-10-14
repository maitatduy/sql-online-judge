import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUserPage } from "./AdminUserPage";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

function renderPage(username: string, search = "") {
    visit(`/admin/users/${username}${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUserPage username={username} />
        </ToastProvider>,
    );
}

const ratingCell = (row: HTMLElement) => within(row).getAllByRole("cell").at(-1);

describe("lịch sử contest trên trang chi tiết", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("tổng quan hiện 3 contest gần nhất và chuyển được sang tab Contest", async () => {
        renderPage("viet.luu");

        const recent = within(await screen.findByRole("region", { name: "Contest gần đây" }));
        const rows = await recent.findAllByRole("row", { name: /^SQL|^Window/ });

        expect(rows.map((row) => within(row).getByRole("rowheader").textContent)).toEqual([
            "SQL Weekly #37",
            "SQL Weekly #36",
            "Window Function Cup",
        ]);

        fireEvent.click(recent.getByRole("button", { name: "Xem tất cả" }));

        expect(window.location.search).toBe("?tab=contests");
    });

    it("ghi rating thay đổi bằng chữ và đánh dấu rating đang tạm giữ", async () => {
        renderPage("viet.luu", "?tab=contests");

        const history = within(await screen.findByRole("region", { name: "Lịch sử contest" }));
        const [held, dropped, gained] = await history.findAllByRole("row", {
            name: /^SQL|^Window/,
        });

        expect(ratingCell(held)).toHaveTextContent("tạm giữ");
        expect(ratingCell(dropped)).toHaveTextContent("Giảm -18");
        expect(ratingCell(gained)).toHaveTextContent("Tăng +9");
        expect(within(dropped).getAllByRole("cell")[0]).toHaveTextContent("604");
    });

    it("phân trang lịch sử contest", async () => {
        renderPage("quang.le", "?tab=contests");

        const history = within(await screen.findByRole("region", { name: "Lịch sử contest" }));

        expect(await history.findByText("1-10 trong 14")).toBeInTheDocument();
        expect(history.getByRole("button", { name: "Trang trước" })).toBeDisabled();

        fireEvent.click(history.getByRole("button", { name: "Trang sau" }));

        expect(await history.findByText("11-14 trong 14")).toBeInTheDocument();
        expect(history.getByRole("button", { name: "Trang sau" })).toBeDisabled();
    });

    it("báo chưa thi contest nào", async () => {
        renderPage("tam.nguyen", "?tab=contests");

        const history = within(await screen.findByRole("region", { name: "Lịch sử contest" }));

        expect(await history.findByText("Chưa tham gia contest nào.")).toBeInTheDocument();
    });
});
