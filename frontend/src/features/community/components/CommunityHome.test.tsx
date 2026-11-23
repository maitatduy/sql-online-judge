import { screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetCommunity } from "@/mocks/community";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { CommunityHome } from "./CommunityHome";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

function renderHome(search = "") {
    visit(`/community${search}`);
    renderWithProviders(
        <ToastProvider>
            <CommunityHome />
        </ToastProvider>,
    );
}

describe("trang cộng đồng", () => {
    beforeEach(() => resetCommunity());

    it("liệt kê bài viết, bài ghim đứng đầu", async () => {
        renderHome();

        const list = await screen.findByRole("list", { name: "Bài viết" });
        const rows = within(list).getAllByRole("listitem");
        expect(rows).toHaveLength(3);
        expect(rows[0]).toHaveTextContent("Khi nào nên dùng window function thay cho GROUP BY");
        expect(screen.getByRole("heading", { level: 1, name: "Cộng đồng" })).toBeVisible();
    });

    it("lọc theo chuyên mục trên URL", async () => {
        renderHome("?category=QUESTION");

        const list = await screen.findByRole("list", { name: "Bài viết" });
        expect(within(list).getAllByRole("listitem")).toHaveLength(1);
        expect(list).toHaveTextContent("LEFT JOIN trả về NULL ở cột đếm");
    });

    it("tab đã lưu khi chưa lưu bài nào thì hiện trạng thái trống", async () => {
        renderHome("?tab=SAVED");

        expect(await screen.findByText("Bạn chưa lưu bài nào")).toBeVisible();
    });
});
