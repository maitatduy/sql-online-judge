import { screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { completeMockLesson, resetMockLearningProgress } from "@/mocks/learning-progress";
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

    it("hiện số bài AC, tỉ lệ AC và ô sắp có cho rating và hạng", async () => {
        renderWithProviders(<LearningDashboard />);

        const stats = within(await screen.findByRole("list", { name: "Thống kê" }));
        expect(await stats.findByText("9")).toBeInTheDocument();
        expect(stats.getByText("+2 tuần này")).toBeInTheDocument();
        expect(stats.getByText("60%")).toBeInTheDocument();
        expect(stats.getByText("20 bài nộp")).toBeInTheDocument();
        expect(stats.getAllByText("Sắp có")).toHaveLength(2);
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

    it("hiện hoạt động 12 tháng gộp bài học và bài nộp, kèm khối sắp có", async () => {
        completeMockLesson("sql-co-ban", "01a1d2c0-0000-7000-8000-000000000201");
        renderWithProviders(<LearningDashboard />);

        expect(
            await screen.findByRole("img", { name: /Lịch hoạt động .* 6 hoạt động/ }),
        ).toBeInTheDocument();
        expect(within(await region("Kỹ năng theo chủ đề")).getByText("Sắp có")).toBeVisible();
        expect(within(await region("Contest sắp tới")).getByText("Sắp có")).toBeVisible();
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
