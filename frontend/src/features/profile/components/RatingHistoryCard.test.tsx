import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeAll, describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { RatingHistoryCard } from "./RatingHistoryCard";

async function preloadChartModule(): Promise<void> {
    await import("./RatingChart");
}

describe("RatingHistoryCard", () => {
    beforeAll(preloadChartModule);

    it("báo đang tải rồi hiện rating của contest mới nhất", async () => {
        renderWithProviders(<RatingHistoryCard username="minh.anh" />);

        expect(screen.getByRole("status", { name: "Đang tải lịch sử rating" })).toHaveAttribute(
            "aria-busy",
            "true",
        );

        expect(
            await screen.findByRole("heading", { level: 2, name: "Rating contest" }),
        ).toBeInTheDocument();
        expect(
            screen.getByText("SQL Judge Weekly #19 · 20/09/2026 · Hạng #36"),
        ).toBeInTheDocument();
    });

    it("có bảng số liệu thay cho biểu đồ với trình đọc màn hình", async () => {
        renderWithProviders(<RatingHistoryCard username="minh.anh" />);

        const table = await screen.findByRole("table", { name: "Rating sau từng contest" });
        const rows = within(table).getAllByRole("row");

        expect(rows).toHaveLength(9);
        expect(rows[1]).toHaveTextContent("1.320");
        expect(rows[1]).toHaveTextContent("#29");
        expect(rows[8]).toHaveTextContent("1.487");
        expect(rows[8]).toHaveTextContent("#36");
    });

    it("người chưa thi thấy trạng thái trống, khách không được mời vào contest", async () => {
        renderWithProviders(<RatingHistoryCard username="newbie" />);

        expect(await screen.findByText("Chưa thi contest nào")).toBeInTheDocument();
        expect(screen.queryByRole("table")).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Xem contest" })).not.toBeInTheDocument();
    });

    it("chủ hồ sơ chưa thi được mời xem contest", async () => {
        writeMockSession({ ...DEMO_SESSION_USER, username: "newbie" });
        renderWithProviders(<RatingHistoryCard username="newbie" />);

        expect(await screen.findByRole("link", { name: "Xem contest" })).toHaveAttribute(
            "href",
            "/contests",
        );
    });

    it("lỗi thì báo riêng trong thẻ và thử lại được", async () => {
        server.use(
            http.get(
                "*/api/users/:username/rating-history",
                () =>
                    HttpResponse.json(
                        {
                            status: 500,
                            code: "INTERNAL_ERROR",
                            message: "Đã có lỗi xảy ra",
                            traceId: "req7f2c91",
                        },
                        { status: 500 },
                    ),
                { once: true },
            ),
        );
        renderWithProviders(<RatingHistoryCard username="minh.anh" />);

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveTextContent("Không tải được lịch sử rating");
        expect(alert).toHaveTextContent("Mã lỗi: req7f2c91");

        fireEvent.click(within(alert).getByRole("button", { name: "Thử lại" }));

        expect(
            await screen.findByRole("heading", { level: 2, name: "Rating contest" }),
        ).toBeInTheDocument();
    });
});
