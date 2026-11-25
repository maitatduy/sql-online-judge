import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { UpcomingContests } from "./UpcomingContests";

function contests(items: object[]) {
    return ({ request }: { request: Request }) => {
        expect(new URL(request.url).searchParams.get("phase")).toBe("UPCOMING");
        return HttpResponse.json({
            items,
            page: 1,
            size: 3,
            totalItems: items.length,
            totalPages: 1,
        });
    };
}

describe("UpcomingContests", () => {
    it("hiện contest sắp diễn ra theo giờ Việt Nam, dẫn tới sảnh chờ", async () => {
        server.use(
            http.get(
                "*/api/contests",
                contests([
                    {
                        id: "c1",
                        slug: "sql-weekly-38",
                        title: "SQL Weekly #38",
                        startAt: "2026-11-28T12:30:00Z",
                        endAt: "2026-11-28T14:00:00Z",
                        durationMinutes: 90,
                        isRated: true,
                        phase: "UPCOMING",
                    },
                ]),
            ),
        );
        renderWithProviders(<UpcomingContests />);

        const link = await screen.findByRole("link", { name: /SQL Weekly #38/ });

        expect(link).toHaveAttribute("href", "/contests/sql-weekly-38");
        expect(link).toHaveTextContent("19:30");
        expect(link).toHaveTextContent("90 phút");
        expect(within(link).getByText("Rated")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Xem tất cả" })).toHaveAttribute(
            "href",
            "/contests",
        );
    });

    it("không có contest sắp tới thì báo trống", async () => {
        server.use(http.get("*/api/contests", contests([])));
        renderWithProviders(<UpcomingContests />);

        expect(await screen.findByText("Chưa có contest nào sắp diễn ra.")).toBeInTheDocument();
    });
});
