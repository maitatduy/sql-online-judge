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

describe("LearningDashboard", () => {
    beforeEach(() => {
        localStorage.clear();
        replaceRoute.mockClear();
        resetMockLearningProgress();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("chào người học và hiện chuỗi ngày, số bài đã học, số khoá đang học", async () => {
        completeMockLesson("sql-co-ban", "01a1d2c0-0000-7000-8000-000000000201");
        renderWithProviders(<LearningDashboard />);

        expect(
            await screen.findByRole("heading", {
                level: 1,
                name: `Chào ${DEMO_SESSION_USER.displayName}`,
            }),
        ).toBeInTheDocument();
        const stats = within(await screen.findByRole("list", { name: "Thống kê học" }));
        expect(await stats.findByText("1 ngày")).toBeInTheDocument();
        expect(stats.getByText("1 bài")).toBeInTheDocument();
        expect(stats.getByText("1 khoá")).toBeInTheDocument();
    });

    it("liệt kê khoá đang học với tiến độ và nút học tiếp", async () => {
        completeMockLesson("sql-co-ban", "01a1d2c0-0000-7000-8000-000000000201");
        renderWithProviders(<LearningDashboard />);

        const course = within(await screen.findByRole("article", { name: "SQL cơ bản" }));
        expect(course.getByText("1/4 bài")).toBeInTheDocument();
        expect(course.getByRole("link", { name: "Học tiếp" })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban/chon-cot",
        );
        expect(
            screen.getByRole("img", { name: /Lịch hoạt động .* 1 bài học/ }),
        ).toBeInTheDocument();
    });

    it("chưa học khoá nào thì mời xem khoá học", async () => {
        renderWithProviders(<LearningDashboard />);

        expect(await screen.findByText("Chưa học khoá nào")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Xem khoá học" })).toHaveAttribute(
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
