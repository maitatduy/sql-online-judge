import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { TopRating } from "./TopRating";

function leaderboard(items: object[]) {
    return () =>
        HttpResponse.json({ items, page: 1, size: 5, totalItems: items.length, totalPages: 1 });
}

describe("TopRating", () => {
    it("hiện thứ hạng, tên dẫn tới hồ sơ và rating định dạng vi-VN", async () => {
        server.use(
            http.get(
                "*/api/leaderboard",
                leaderboard([
                    { userId: "u1", username: "quang.le", displayName: null, rating: 2184 },
                    { userId: "u2", username: "trang.hoang", displayName: null, rating: 2097 },
                ]),
            ),
        );
        renderWithProviders(<TopRating />);

        const rows = within(await screen.findByRole("list")).getAllByRole("listitem");

        expect(rows).toHaveLength(2);
        expect(rows[0]).toHaveTextContent("1");
        expect(rows[0]).toHaveTextContent("2.184");
        expect(within(rows[0]).getByRole("link", { name: "quang.le" })).toHaveAttribute(
            "href",
            "/u/quang.le",
        );
    });

    it("chưa ai có rating thì mời thi contest", async () => {
        server.use(http.get("*/api/leaderboard", leaderboard([])));
        renderWithProviders(<TopRating />);

        expect(
            await screen.findByText("Chưa ai có rating. Thi một contest để lên bảng đầu tiên."),
        ).toBeInTheDocument();
    });
});
