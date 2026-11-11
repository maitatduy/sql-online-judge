import { screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { clearMockSession, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
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
        ["Lộ trình", "/learn"],
        ["Bài tập", "/problems"],
        ["Contest", "/contests"],
        ["Cộng đồng", "/community"],
    ])("link điều hướng %s trỏ tới %s", (name, href) => {
        renderWithProviders(<MarketingHeader />);

        const nav = screen.getByRole("navigation", { name: "Giới thiệu" });

        expect(within(nav).getByRole("link", { name })).toHaveAttribute("href", href);
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

    it("đã đăng nhập thì vào thẳng luyện tập và thấy tài khoản của mình", async () => {
        writeMockSession(DEMO_SESSION_USER);

        renderWithProviders(<MarketingHeader />);

        expect(await screen.findByRole("link", { name: "Tiếp tục luyện tập" })).toHaveAttribute(
            "href",
            "/problems",
        );
        expect(screen.getByRole("link", { name: "Hồ sơ của tôi" })).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Đăng xuất" })).toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Đăng nhập" })).not.toBeInTheDocument();
        expect(screen.queryByRole("link", { name: "Bắt đầu miễn phí" })).not.toBeInTheDocument();
    });
});
