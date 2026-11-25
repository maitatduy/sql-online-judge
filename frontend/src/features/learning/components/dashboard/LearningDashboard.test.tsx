import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { completeMockLesson, resetMockLearningProgress } from "@/mocks/learning-progress";
import { server } from "@/mocks/server";
import { clearMockSession, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { LearningDashboard } from "./LearningDashboard";

const replaceRoute = vi.fn();

vi.mock("next/navigation", () => ({
    usePathname: () => "/dashboard",
    useRouter: () => ({ replace: replaceRoute }),
}));

function region(name: string) {
    return screen.findByRole("region", { name });
}

describe("LearningDashboard", () => {
    beforeEach(() => {
        localStorage.clear();
        replaceRoute.mockClear();
        resetMockLearningProgress();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("chào theo buổi và nhắc chuỗi ngày học", async () => {
        completeMockLesson("sql-co-ban", "01a1d2c0-0000-7000-8000-000000000201");
        renderWithProviders(<LearningDashboard />);

        expect(
            await screen.findByRole("heading", {
                level: 1,
                name: new RegExp(`^Chào buổi (sáng|chiều|tối), ${DEMO_SESSION_USER.displayName}$`),
            }),
        ).toBeInTheDocument();
        expect(await screen.findByText("3 ngày")).toBeInTheDocument();
    });

    it("hiện số bài AC, tỉ lệ AC, rating và hạng toàn cục", async () => {
        server.use(
            http.get("*/api/leaderboard/:userId", ({ params }) =>
                HttpResponse.json({
                    userId: params.userId,
                    rating: 1650,
                    rank: 12,
                    contestsPlayed: 4,
                }),
            ),
        );
        renderWithProviders(<LearningDashboard />);

        const stats = within(await screen.findByRole("list", { name: "Thống kê" }));
        expect(await stats.findByText("9")).toBeInTheDocument();
        expect(stats.getByText("+2 tuần này")).toBeInTheDocument();
        expect(stats.getByText("60%")).toBeInTheDocument();
        expect(stats.getByText("20 bài nộp")).toBeInTheDocument();
        expect(await stats.findByText("1.650")).toBeInTheDocument();
        expect(stats.getByText("4 contest đã thi")).toBeInTheDocument();
        expect(stats.getByText("#12")).toBeInTheDocument();
    });

    it("chưa thi contest tính rating thì rating và hạng để trống kèm lời nhắc", async () => {
        server.use(
            http.get("*/api/leaderboard/:userId", ({ params }) =>
                HttpResponse.json({
                    userId: params.userId,
                    rating: null,
                    rank: null,
                    contestsPlayed: 0,
                }),
            ),
        );
        renderWithProviders(<LearningDashboard />);

        const stats = within(await screen.findByRole("list", { name: "Thống kê" }));
        expect(await stats.findByText("Chưa thi contest tính rating")).toBeInTheDocument();
        expect(stats.getAllByText("-")).toHaveLength(2);
    });

    it("học tiếp khoá đang học và gợi ý bài luyện tập hôm nay", async () => {
        completeMockLesson("sql-co-ban", "01a1d2c0-0000-7000-8000-000000000201");
        renderWithProviders(<LearningDashboard />);

        const learning = within(await region("Học tiếp"));
        expect(await learning.findByText("SQL cơ bản")).toBeInTheDocument();
        expect(learning.getByRole("link", { name: "Tiếp tục" })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban/chon-cot",
        );
        const challenge = within(await region("Thử thách hôm nay"));
        await waitFor(() =>
            expect(
                challenge
                    .getAllByRole("link")
                    .filter((link) => link.getAttribute("href")?.startsWith("/problems/")).length,
            ).toBeGreaterThan(1),
        );
    });

    it("liệt kê bài nộp gần đây với verdict và tên bài", async () => {
        renderWithProviders(<LearningDashboard />);

        const recent = within(await region("Bài nộp gần đây"));
        const rows = await recent.findAllByRole("row");
        expect(rows).toHaveLength(3);
        expect(within(rows[1]).getByText("AC")).toBeInTheDocument();
        expect(
            within(rows[1]).getByRole("link", { name: "#1 Tổng lương mỗi phòng ban" }),
        ).toHaveAttribute("href", "/problems/tong-luong-moi-phong-ban");
    });

    it("hiện hoạt động 12 tháng gộp bài học và bài nộp", async () => {
        completeMockLesson("sql-co-ban", "01a1d2c0-0000-7000-8000-000000000201");
        renderWithProviders(<LearningDashboard />);

        expect(
            await screen.findByRole("img", { name: /Lịch hoạt động .* 6 hoạt động/ }),
        ).toBeInTheDocument();
    });

    it("kỹ năng theo chủ đề hiện số bài đã AC trên tổng bài và dẫn tới danh sách bài", async () => {
        server.use(
            http.get("*/api/solved-summaries/:userId/topics", ({ params }) =>
                HttpResponse.json({
                    userId: params.userId,
                    topics: [
                        { slug: "join", name: "JOIN", solved: 3, total: 8 },
                        { slug: "cte", name: "CTE", solved: 0, total: 2 },
                    ],
                }),
            ),
        );
        renderWithProviders(<LearningDashboard />);

        const skills = within(await region("Kỹ năng theo chủ đề"));
        expect(await skills.findByRole("link", { name: "JOIN" })).toHaveAttribute(
            "href",
            "/problems?tag=join",
        );
        expect(skills.getByText("3/8")).toBeInTheDocument();
        expect(
            skills.getByRole("progressbar", { name: "JOIN: đã AC 3 trên 8 bài" }),
        ).toHaveAttribute("aria-valuenow", "3");
    });

    it("contest sắp tới dẫn tới trang contest", async () => {
        server.use(
            http.get("*/api/contests", () =>
                HttpResponse.json({
                    items: [
                        {
                            id: "contest-1",
                            slug: "sql-weekly-40",
                            title: "SQL Weekly #40",
                            startAt: "2026-12-01T13:00:00Z",
                            durationMinutes: 90,
                            isRated: true,
                        },
                    ],
                    page: 1,
                    size: 3,
                    totalItems: 1,
                    totalPages: 1,
                }),
            ),
        );
        renderWithProviders(<LearningDashboard />);

        const contests = within(await region("Contest sắp tới"));
        expect(await contests.findByRole("link", { name: /SQL Weekly #40/ })).toHaveAttribute(
            "href",
            "/contests/sql-weekly-40",
        );
        expect(contests.getByText(/90 phút$/)).toBeInTheDocument();
    });

    it("chưa học khoá nào thì mời xem khoá học", async () => {
        renderWithProviders(<LearningDashboard />);

        const learning = within(await region("Học tiếp"));
        expect(await learning.findByRole("link", { name: "Xem khoá học" })).toHaveAttribute(
            "href",
            "/learn",
        );
    });

    it("chuyển sang đăng nhập khi chưa đăng nhập", async () => {
        clearMockSession();
        renderWithProviders(<LearningDashboard />);

        await waitFor(() => expect(replaceRoute).toHaveBeenCalledWith("/login?next=%2Fdashboard"));
    });
});
