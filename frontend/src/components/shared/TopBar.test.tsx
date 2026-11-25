import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
    ADMIN_SESSION_USER,
    clearMockSession,
    DEMO_SESSION_USER,
    SETTER_SESSION_USER,
    writeMockSession,
} from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { TopBar } from "./TopBar";

let pathname = "/";

vi.mock("next/navigation", () => ({
    usePathname: () => pathname,
    useRouter: () => ({ push: vi.fn() }),
}));

describe("TopBar", () => {
    beforeEach(() => {
        clearMockSession();
        pathname = "/";
    });

    it("logo dẫn về trang chủ", () => {
        renderWithProviders(<TopBar />);

        expect(screen.getByRole("link", { name: "SQL Online Judge" })).toHaveAttribute("href", "/");
    });

    it.each([
        ["Học", "/learn"],
        ["Luyện tập", "/problems"],
        ["Contest", "/contests"],
        ["Cộng đồng", "/community"],
        ["Xếp hạng", "/leaderboard"],
    ])("link điều hướng %s trỏ tới %s", (name, href) => {
        renderWithProviders(<TopBar />);

        const nav = screen.getByRole("navigation", { name: "Điều hướng chính" });

        expect(within(nav).getByRole("link", { name })).toHaveAttribute("href", href);
        expect(within(nav).queryByRole("link", { name: "Lộ trình" })).not.toBeInTheDocument();
    });

    it("người ra đề thấy lối vào Studio, admin thấy thêm Quản trị", async () => {
        writeMockSession(SETTER_SESSION_USER);
        const { unmount } = renderWithProviders(<TopBar />);
        const setterNav = screen.getByRole("navigation", { name: "Điều hướng chính" });

        expect(await within(setterNav).findByRole("link", { name: "Ra đề" })).toHaveAttribute(
            "href",
            "/studio/problems",
        );
        expect(within(setterNav).getByRole("link", { name: "Tạo khoá học" })).toHaveAttribute(
            "href",
            "/studio/courses",
        );
        expect(within(setterNav).queryByRole("link", { name: "Quản trị" })).not.toBeInTheDocument();
        unmount();

        writeMockSession(ADMIN_SESSION_USER);
        renderWithProviders(<TopBar />);
        const adminNav = screen.getByRole("navigation", { name: "Điều hướng chính" });

        expect(await within(adminNav).findByRole("link", { name: "Quản trị" })).toHaveAttribute(
            "href",
            "/admin",
        );
    });

    it("nút menu mở danh sách trang trên điện thoại", async () => {
        renderWithProviders(<TopBar />);

        fireEvent.click(screen.getByRole("button", { name: "Mở menu" }));

        const menu = await screen.findByRole("navigation", { name: "Menu điện thoại" });
        expect(within(menu).getByRole("link", { name: "Luyện tập" })).toHaveAttribute(
            "href",
            "/problems",
        );
    });

    it("chưa đăng nhập thì có nút đăng nhập và đăng ký", async () => {
        renderWithProviders(<TopBar />);

        expect(await screen.findByRole("link", { name: "Đăng nhập" })).toHaveAttribute(
            "href",
            "/login",
        );
        expect(screen.getByRole("link", { name: "Bắt đầu miễn phí" })).toHaveAttribute(
            "href",
            "/register",
        );
    });

    it("đã đăng nhập thì chỉ thấy tài khoản của mình, không có nút tiếp tục luyện tập", async () => {
        writeMockSession(DEMO_SESSION_USER);

        renderWithProviders(<TopBar />);

        expect(await screen.findByRole("link", { name: "Hồ sơ của tôi" })).toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Tiếp tục luyện tập" })).not.toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Đăng xuất" })).toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Đăng nhập" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Bắt đầu miễn phí" })).not.toBeInTheDocument();
    });

    it("đăng xuất thì ẩn ngay các mục theo vai trò và hiện lại nút đăng nhập", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        renderWithProviders(<TopBar />);
        const nav = screen.getByRole("navigation", { name: "Điều hướng chính" });
        expect(await within(nav).findByRole("link", { name: "Quản trị" })).toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "Đăng xuất" }));

        expect(await screen.findByRole("link", { name: "Đăng nhập" })).toBeInTheDocument();
        expect(within(nav).queryByRole("link", { name: "Quản trị" })).not.toBeInTheDocument();
        expect(within(nav).queryByRole("link", { name: "Ra đề" })).not.toBeInTheDocument();
    });

    it("đã đăng nhập thì có mục Tổng quan dẫn tới dashboard và đánh dấu trang đang mở", async () => {
        writeMockSession(DEMO_SESSION_USER);
        pathname = "/dashboard";
        renderWithProviders(<TopBar />);

        const nav = screen.getByRole("navigation", { name: "Điều hướng chính" });
        const dashboard = await within(nav).findByRole("link", { name: "Tổng quan" });

        expect(dashboard).toHaveAttribute("href", "/dashboard");
        expect(dashboard).toHaveAttribute("aria-current", "page");
        expect(within(nav).getByRole("link", { name: "Học" })).not.toHaveAttribute("aria-current");
    });

    it("chưa đăng nhập thì không có mục Tổng quan", () => {
        renderWithProviders(<TopBar />);

        const nav = screen.getByRole("navigation", { name: "Điều hướng chính" });

        expect(within(nav).queryByRole("link", { name: "Tổng quan" })).not.toBeInTheDocument();
    });

    it("không có ô tìm kiếm, có nút thông báo khi đã đăng nhập", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<TopBar />);

        expect(await screen.findByRole("button", { name: /^Thông báo/ })).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Trợ lý AI" })).toHaveAttribute("href", "/ai");
        expect(screen.queryByRole("searchbox")).not.toBeInTheDocument();
    });
});
