import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";

import { mockContests, resetAdminContests } from "@/mocks/admin-contests";
import { registerForMockContest, resetContestRegistrations } from "@/mocks/contests";
import { server } from "@/mocks/server";
import { clearMockSession, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ContestCatalogue } from "./ContestCatalogue";

describe("danh sách contest", () => {
    beforeEach(() => {
        localStorage.clear();
        resetAdminContests();
        resetContestRegistrations();
        clearMockSession();
    });

    it("có breadcrumb về trang chủ", async () => {
        renderWithProviders(<ContestCatalogue />);

        const breadcrumb = within(screen.getByRole("navigation", { name: "Đường dẫn" }));
        expect(breadcrumb.getByRole("link", { name: "Trang chủ" })).toHaveAttribute("href", "/");
        expect(breadcrumb.getByText("Contest")).toHaveAttribute("aria-current", "page");
        await screen.findByRole("article", { name: "SQL Cup 2" });
    });

    it("thẻ sắp diễn ra ghi số người đăng ký, số bài và làm nổi bật contest đã đăng ký", async () => {
        writeMockSession(DEMO_SESSION_USER);
        registerForMockContest("sql-cup-2", DEMO_SESSION_USER.id);
        registerForMockContest("sql-cup-2", "nguoi-khac");
        const problemCount =
            mockContests().find((contest) => contest.slug === "sql-cup-2")?.problems.length ?? 0;
        renderWithProviders(<ContestCatalogue />);

        const card = within(await screen.findByRole("article", { name: "SQL Cup 2" }));
        expect(card.getByText("2 người đăng ký")).toBeInTheDocument();
        expect(card.getByText(`${problemCount} bài`)).toBeInTheDocument();
        expect(card.getByText("Đã đăng ký")).toBeInTheDocument();
    });

    it("bảng đã kết thúc có số bài và số người tham gia", async () => {
        renderWithProviders(<ContestCatalogue />);
        await screen.findByRole("article", { name: "SQL Cup 2" });

        fireEvent.mouseDown(screen.getByRole("tab", { name: /Đã kết thúc/ }));

        const table = within(await screen.findByRole("table", { name: "Contest đã kết thúc" }));
        expect(table.getByRole("columnheader", { name: "Số bài" })).toBeInTheDocument();
        expect(table.getByRole("columnheader", { name: "Người tham gia" })).toBeInTheDocument();
    });

    it("chưa có contest nào đang diễn ra thì hiện khối thông báo kèm lối sang luyện tập", async () => {
        server.use(
            http.get("*/api/contests", ({ request }) =>
                new URL(request.url).searchParams.get("phase") === "RUNNING"
                    ? HttpResponse.json({
                          items: [],
                          page: 1,
                          size: 20,
                          totalItems: 0,
                          totalPages: 0,
                      })
                    : undefined,
            ),
        );
        renderWithProviders(<ContestCatalogue />);

        const empty = within(
            await screen.findByRole("region", { name: "Chưa có contest đang diễn ra" }),
        );
        expect(empty.getByRole("link", { name: "Luyện tập trong lúc chờ" })).toHaveAttribute(
            "href",
            "/problems",
        );
    });

    it("đưa contest đang diễn ra lên khối nổi bật có đồng hồ và nút vào thi", async () => {
        renderWithProviders(<ContestCatalogue />);

        const live = within(await screen.findByRole("region", { name: "SQL Cup đang thi" }));
        expect(live.getByText("Đang diễn ra")).toBeInTheDocument();
        expect(live.getByRole("timer", { name: "Còn lại" })).toBeInTheDocument();
        expect(live.getByRole("link", { name: "Vào thi" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-live",
        );
    });

    it("tab sắp diễn ra có thẻ ngày, tab đã kết thúc liệt kê contest cũ", async () => {
        renderWithProviders(<ContestCatalogue />);

        const upcoming = await screen.findByRole("article", { name: "SQL Cup 2" });
        expect(within(upcoming).getByRole("link", { name: "SQL Cup 2" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-2",
        );
        expect(within(upcoming).getByText("Tính rating")).toBeInTheDocument();
        expect(within(upcoming).getByText(/90 phút/)).toBeInTheDocument();

        fireEvent.mouseDown(screen.getByRole("tab", { name: /Đã kết thúc/ }));

        expect(await screen.findByRole("link", { name: "SQL Cup 1" })).toHaveAttribute(
            "href",
            "/contests/sql-cup-1",
        );
    });
});
