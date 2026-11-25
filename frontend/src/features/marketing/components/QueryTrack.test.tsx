import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { QueryTrack } from "./QueryTrack";

const COUNTS: Record<string, number> = { "select-where": 0, join: 3, "group-by": 12 };

function countsByTag() {
    server.use(
        http.get("*/api/problems", ({ request }) => {
            const tag = new URL(request.url).searchParams.get("tag") ?? "";
            return HttpResponse.json({
                items: [],
                page: 1,
                size: 1,
                totalItems: COUNTS[tag] ?? 1,
                totalPages: 1,
            });
        }),
    );
}

describe("QueryTrack", () => {
    it("có sáu trạm, trạm đầu được chọn và có câu SQL mẫu", async () => {
        countsByTag();
        renderWithProviders(<QueryTrack />);

        const tabs = screen.getAllByRole("tab");

        expect(tabs.map((tab) => tab.textContent)).toEqual([
            "SELECT",
            "JOIN",
            "GROUP BY",
            "SUBQUERY",
            "WITH",
            "OVER()",
        ]);
        expect(tabs[0]).toHaveAttribute("aria-selected", "true");
        expect(
            screen.getByRole("figure", { name: "Câu truy vấn mẫu cho trạm SELECT" }),
        ).toBeInTheDocument();
        expect(await screen.findByText("0 bài có máy chấm")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Xem bài tập" })).toHaveAttribute(
            "href",
            "/problems?tag=select-where",
        );
    });

    it("chọn trạm khác thì đổi câu mẫu và số bài thật của chủ đề", async () => {
        countsByTag();
        renderWithProviders(<QueryTrack />);

        fireEvent.click(screen.getByRole("tab", { name: "JOIN" }));

        const panel = screen.getByRole("tabpanel");
        expect(within(panel).getByRole("heading", { name: "Nối bảng" })).toBeInTheDocument();
        expect(await within(panel).findByRole("link", { name: "Luyện 3 bài" })).toHaveAttribute(
            "href",
            "/problems?tag=join",
        );
    });

    it("đi giữa các trạm bằng phím mũi tên", () => {
        countsByTag();
        renderWithProviders(<QueryTrack />);

        const tablist = screen.getByRole("tablist", { name: "Các trạm của lộ trình" });
        fireEvent.keyDown(tablist, { key: "ArrowRight" });
        fireEvent.keyDown(tablist, { key: "ArrowRight" });

        expect(screen.getByRole("tab", { name: "GROUP BY" })).toHaveAttribute(
            "aria-selected",
            "true",
        );
        expect(screen.getByRole("tab", { name: "GROUP BY" })).toHaveFocus();
        fireEvent.keyDown(tablist, { key: "End" });
        expect(screen.getByRole("tab", { name: "OVER()" })).toHaveAttribute(
            "aria-selected",
            "true",
        );
    });
});
