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

import { MarketingHeader } from "./MarketingHeader";

vi.mock("next/navigation", () => ({
    usePathname: () => "/",
    useRouter: () => ({ push: vi.fn() }),
}));

describe("MarketingHeader", () => {
    beforeEach(() => {
        clearMockSession();
    });

    it("logo dẫn về trang chủ", () => {
        renderWithProviders(<MarketingHeader />);

        expect(screen.getByRole("link", { name: "SQL Online Judge" })).toHaveAttribute("href", "/");
    });

    it.each([
        ["Học", "/learn"],
        ["Luyện tập", "/problems"],
        ["Contest", "/contests"],
        ["Cộng đồng", "/community"],
    ])("link điều hướng %s trỏ tới %s", (name, href) => {
        renderWithProviders(<MarketingHeader />);

        const nav = screen.getByRole("navigation", { name: "Điều hướng chính" });

        expect(within(nav).getByRole("link", { name })).toHaveAttribute("href", href);
        expect(within(nav).queryByRole("link", { name: "Lộ trình" })).not.toBeInTheDocument();
    });

    it("người ra đề thấy lối vào Studio, admin thấy thêm Quản trị", async () => {
        writeMockSession(SETTER_SESSION_USER);
        const { unmount } = renderWithProviders(<MarketingHeader />);
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
        renderWithProviders(<MarketingHeader />);
        const adminNav = screen.getByRole("navigation", { name: "Điều hướng chính" });

        expect(await within(adminNav).findByRole("link", { name: "Quản trị" })).toHaveAttribute(
            "href",
            "/admin",
        );
    });

    it("nút menu mở danh sách trang trên điện thoại", async () => {
        renderWithProviders(<MarketingHeader />);

        fireEvent.click(screen.getByRole("button", { name: "Mở menu" }));

        const menu = await screen.findByRole("navigation", { name: "Menu điện thoại" });
        expect(within(menu).getByRole("link", { name: "Luyện tập" })).toHaveAttribute(
            "href",
            "/problems",
        );
    });

    it("chưa đăng nhập thì có nút đăng nhập và đăng ký", async () => {
        renderWithProviders(<MarketingHeader />);

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

        renderWithProviders(<MarketingHeader />);

        expect(await screen.findByRole("link", { name: "Hồ sơ của tôi" })).toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Tiếp tục luyện tập" })).not.toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Đăng xuất" })).toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Đăng nhập" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Bắt đầu miễn phí" })).not.toBeInTheDocument();
    });

    it("đăng xuất thì ẩn ngay các mục theo vai trò và hiện lại nút đăng nhập", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        renderWithProviders(<MarketingHeader />);
        const nav = screen.getByRole("navigation", { name: "Điều hướng chính" });
        expect(await within(nav).findByRole("link", { name: "Quản trị" })).toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "Đăng xuất" }));

        expect(await screen.findByRole("link", { name: "Đăng nhập" })).toBeInTheDocument();
        expect(within(nav).queryByRole("link", { name: "Quản trị" })).not.toBeInTheDocument();
        expect(within(nav).queryByRole("link", { name: "Ra đề" })).not.toBeInTheDocument();
    });
});
