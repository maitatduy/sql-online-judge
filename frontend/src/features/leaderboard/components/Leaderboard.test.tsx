import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { Leaderboard } from "./Leaderboard";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

describe("bảng xếp hạng", () => {
    beforeEach(() => visit("/leaderboard"));

    it("liệt kê người thi theo hạng, tên dẫn tới hồ sơ", async () => {
        renderWithProviders(<Leaderboard />);

        const rows = within(await screen.findByRole("table")).getAllByRole("row");
        expect(rows).toHaveLength(5);
        expect(rows[1]).toHaveTextContent("1");
        expect(rows[1]).toHaveTextContent("1.820");
        expect(within(rows[1]).getByRole("link", { name: "Bùi An" })).toHaveAttribute(
            "href",
            "/u/an.bui",
        );
    });

    it("chưa có ai thi thì hiện trạng thái trống", async () => {
        server.use(
            http.get("*/api/leaderboard", () =>
                HttpResponse.json({ items: [], page: 1, size: 50, totalItems: 0, totalPages: 1 }),
            ),
        );
        renderWithProviders(<Leaderboard />);

        expect(await screen.findByText("Chưa có ai thi contest tính rating.")).toBeVisible();
    });
});
