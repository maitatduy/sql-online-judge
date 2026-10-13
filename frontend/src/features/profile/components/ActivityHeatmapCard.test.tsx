import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { ActivityHeatmapCard } from "./ActivityHeatmapCard";

const NOW = new Date("2026-10-13T05:00:00Z");

function respondWithActivity() {
    server.use(
        http.get("*/api/users/:username/activity", () =>
            HttpResponse.json({
                totalSubmissions: 1214,
                days: [
                    { date: "2026-10-13", count: 3 },
                    { date: "2026-10-12", count: 7 },
                ],
            }),
        ),
    );
}

describe("ActivityHeatmapCard", () => {
    it("báo đang tải rồi hiện tổng số bài nộp và chuỗi dài nhất", async () => {
        respondWithActivity();
        renderWithProviders(
            <ActivityHeatmapCard username="minh.anh" longestStreakDays={31} now={NOW} />,
        );

        expect(screen.getByRole("status", { name: "Đang tải hoạt động" })).toHaveAttribute(
            "aria-busy",
            "true",
        );

        expect(
            await screen.findByRole("heading", { level: 2, name: "1.214 bài nộp trong năm qua" }),
        ).toBeInTheDocument();
        expect(screen.getByText("Chuỗi dài nhất 31 ngày")).toBeInTheDocument();
    });

    it("mỗi ô ghi ngày và số bài nộp, lưới có mô tả cho trình đọc màn hình", async () => {
        respondWithActivity();
        renderWithProviders(
            <ActivityHeatmapCard username="minh.anh" longestStreakDays={31} now={NOW} />,
        );

        const grid = await screen.findByRole("img", {
            name: "Lịch hoạt động 52 tuần gần nhất, 1.214 bài nộp",
        });

        expect(within(grid).getByTitle("13/10/2026: 3 bài nộp")).toHaveAttribute("data-level", "2");
        expect(within(grid).getByTitle("12/10/2026: 7 bài nộp")).toHaveAttribute("data-level", "4");
        expect(within(grid).getByTitle("11/10/2026: 0 bài nộp")).toHaveAttribute("data-level", "0");
        expect(within(grid).queryByTitle(/^14\/10\/2026/)).not.toBeInTheDocument();
    });

    it("vùng cuộn ngang của lưới tới được bằng bàn phím", async () => {
        respondWithActivity();
        renderWithProviders(
            <ActivityHeatmapCard username="minh.anh" longestStreakDays={31} now={NOW} />,
        );

        expect(
            await screen.findByRole("region", { name: "Lịch hoạt động, cuộn ngang để xem hết" }),
        ).toHaveAttribute("tabindex", "0");
    });

    it("người mới vẫn thấy lưới rỗng với tổng 0", async () => {
        renderWithProviders(
            <ActivityHeatmapCard username="newbie" longestStreakDays={0} now={NOW} />,
        );

        expect(
            await screen.findByRole("heading", { level: 2, name: "0 bài nộp trong năm qua" }),
        ).toBeInTheDocument();
        expect(screen.getByTitle("13/10/2026: 0 bài nộp")).toBeInTheDocument();
    });

    it("lỗi thì báo riêng trong thẻ và thử lại được", async () => {
        server.use(
            http.get(
                "*/api/users/:username/activity",
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
        renderWithProviders(
            <ActivityHeatmapCard username="newbie" longestStreakDays={0} now={NOW} />,
        );

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveTextContent("Không tải được hoạt động");
        expect(alert).toHaveTextContent("Mã lỗi: req7f2c91");

        fireEvent.click(within(alert).getByRole("button", { name: "Thử lại" }));

        expect(
            await screen.findByRole("heading", { level: 2, name: "0 bài nộp trong năm qua" }),
        ).toBeInTheDocument();
    });
});
