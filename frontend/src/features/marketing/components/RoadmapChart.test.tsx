import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { RoadmapChart } from "./RoadmapChart";

const COUNTS: Record<string, number> = { "select-where": 0, join: 3 };

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

describe("RoadmapChart", () => {
    it("vẽ sáu chủ đề chính theo thứ tự, kèm số bài thật", async () => {
        countsByTag();
        renderWithProviders(<RoadmapChart />);

        const topics = screen.getAllByRole("button", { name: /^0\d/ });

        const titles = [
            "Truy vấn cơ bản",
            "Nối bảng",
            "Gom nhóm",
            "Truy vấn lồng",
            "CTE",
            "Window function",
        ];
        expect(topics).toHaveLength(titles.length);
        topics.forEach((topic, index) => expect(topic).toHaveTextContent(titles[index]));
        expect(await within(topics[1]).findByText("3 bài có máy chấm")).toBeInTheDocument();
        expect(screen.getAllByText("LEFT JOIN").length).toBeGreaterThan(0);
    });

    it("bấm một chủ đề mở bảng chi tiết có câu mẫu và nút luyện bài", async () => {
        countsByTag();
        renderWithProviders(<RoadmapChart />);

        fireEvent.click(screen.getByRole("button", { name: /Nối bảng/ }));

        const sheet = await screen.findByRole("dialog", { name: "Nối bảng" });
        expect(
            within(sheet).getByRole("figure", { name: "Câu truy vấn mẫu cho chủ đề Nối bảng" }),
        ).toBeInTheDocument();
        expect(await within(sheet).findByRole("link", { name: "Luyện 3 bài" })).toHaveAttribute(
            "href",
            "/problems?tag=join",
        );
    });

    it("chủ đề chưa có bài thì mời xem danh sách bài", async () => {
        countsByTag();
        renderWithProviders(<RoadmapChart />);

        fireEvent.click(screen.getByRole("button", { name: /Truy vấn cơ bản/ }));

        const sheet = await screen.findByRole("dialog");
        expect(await within(sheet).findByRole("link", { name: "Xem bài tập" })).toHaveAttribute(
            "href",
            "/problems?tag=select-where",
        );
    });
});
