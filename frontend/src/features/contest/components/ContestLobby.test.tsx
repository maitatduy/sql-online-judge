import { fireEvent, screen, within } from "@testing-library/react";
import { http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { apiError } from "@/mocks/api-error";
import { resetAdminContests } from "@/mocks/admin-contests";
import { resetContestRegistrations } from "@/mocks/contests";
import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ContestLobby } from "./ContestLobby";

vi.mock("next/navigation", () => ({
    usePathname: () => "/contests/sql-cup-2",
}));

function renderLobby(slug: string) {
    renderWithProviders(
        <ToastProvider>
            <ContestLobby slug={slug} />
        </ToastProvider>,
    );
}

function participants() {
    return screen.getByRole("region", { name: "Người tham gia" });
}

describe("sảnh chờ contest", () => {
    beforeEach(() => {
        localStorage.clear();
        resetAdminContests();
        resetContestRegistrations();
    });

    it("người chưa đăng nhập thấy thông tin và nút đăng nhập để đăng ký", async () => {
        renderLobby("sql-cup-2");

        expect(await screen.findByRole("heading", { name: "SQL Cup 2" })).toBeInTheDocument();
        expect(screen.getByText("Sắp diễn ra")).toBeInTheDocument();
        expect(participants()).toHaveTextContent("0");
        expect(screen.getByRole("timer", { name: "Bắt đầu sau" })).toBeInTheDocument();
        expect(screen.getByText(/phạt 20 phút/)).toBeInTheDocument();
        expect(screen.getByText(/đóng băng 30 phút cuối/)).toBeInTheDocument();
        expect(screen.getByText(/Đề bài mở lúc/)).toBeInTheDocument();
        expect(await screen.findByRole("link", { name: "Đăng nhập để đăng ký" })).toHaveAttribute(
            "href",
            "/login?next=%2Fcontests%2Fsql-cup-2",
        );
    });

    it("đăng ký rồi huỷ đăng ký trước giờ thi", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderLobby("sql-cup-2");

        fireEvent.click(await screen.findByRole("button", { name: "Đăng ký" }));

        expect(await screen.findByText("Bạn đã đăng ký")).toBeInTheDocument();
        expect(participants()).toHaveTextContent("1");

        fireEvent.click(screen.getByRole("button", { name: "Huỷ đăng ký" }));

        expect(await screen.findByRole("button", { name: "Đăng ký" })).toBeInTheDocument();
        expect(participants()).toHaveTextContent("0");
    });

    it("contest đang diễn ra cho đăng ký muộn nhưng không cho huỷ", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderLobby("sql-cup-live");

        fireEvent.click(await screen.findByRole("button", { name: "Đăng ký" }));

        expect(await screen.findByText("Bạn đã đăng ký")).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Huỷ đăng ký" })).toBeNull();
        expect(screen.getByRole("timer", { name: "Còn lại" })).toBeInTheDocument();
        const problems = within(await screen.findByRole("table", { name: "Bài trong contest" }));
        expect(problems.getByText("Lương cao nhất mỗi phòng ban")).toBeInTheDocument();
    });

    it("contest đã kết thúc không còn nút đăng ký", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderLobby("sql-cup-1");

        expect(await screen.findByText("Đã kết thúc")).toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Đăng ký" })).toBeNull();
        const problems = within(await screen.findByRole("table", { name: "Bài trong contest" }));
        expect(problems.getAllByRole("row")).toHaveLength(3);
    });

    it("tài khoản bị cấm thi contest tính rating thấy lý do", async () => {
        writeMockSession(DEMO_SESSION_USER);
        server.use(
            http.post("*/api/contests/:slug/registration", () =>
                apiError(
                    403,
                    "RATED_CONTEST_BANNED",
                    "Tài khoản đang bị cấm tham gia contest tính rating",
                ),
            ),
        );
        renderLobby("sql-cup-2");

        fireEvent.click(await screen.findByRole("button", { name: "Đăng ký" }));

        expect(
            await screen.findByText("Tài khoản đang bị cấm tham gia contest tính rating"),
        ).toBeInTheDocument();
    });

    it("hiện lỗi khi không có contest", async () => {
        renderLobby("khong-co");

        expect(await screen.findByRole("alert")).toHaveTextContent("Không tìm thấy contest");
    });
});
