import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { StatsStrip } from "./StatsStrip";

function total(totalItems: number) {
    return () => HttpResponse.json({ items: [], page: 1, size: 1, totalItems, totalPages: 1 });
}

describe("StatsStrip", () => {
    it("lấy tổng số từ API và định dạng theo vi-VN", async () => {
        server.use(
            http.get("*/api/problems", total(1806)),
            http.get("*/api/courses", total(12)),
            http.get("*/api/contests", total(38)),
            http.get("*/api/leaderboard", total(2450)),
        );
        renderWithProviders(<StatsStrip />);

        expect(screen.getByRole("status", { name: "Đang tải số liệu" })).toBeInTheDocument();
        const stats = await screen.findByRole("region", { name: "Số liệu nổi bật" });
        const values = await within(stats).findAllByRole("definition");

        expect(values.map((value) => value.textContent)).toEqual(["1.806", "12", "38", "2.450"]);
        expect(within(stats).getByText("bài tập có máy chấm")).toBeInTheDocument();
        expect(within(stats).getByText("người có rating")).toBeInTheDocument();
    });

    it("lỗi thì báo kèm nút thử lại", async () => {
        server.use(
            http.get("*/api/problems", () =>
                HttpResponse.json(
                    { status: 503, code: "SERVICE_UNAVAILABLE", message: "Lỗi" },
                    { status: 503 },
                ),
            ),
        );
        renderWithProviders(<StatsStrip />);

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveTextContent("Chưa tải được số liệu");
        expect(within(alert).getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });
});
