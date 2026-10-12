import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MarketingHeader } from "./MarketingHeader";

describe("MarketingHeader", () => {
    it("logo dẫn về trang chủ", () => {
        render(<MarketingHeader />);

        expect(screen.getByRole("link", { name: "SQL Online Judge" })).toHaveAttribute("href", "/");
    });

    it.each([
        ["Lộ trình", "/learn"],
        ["Bài tập", "/problems"],
        ["Contest", "/contests"],
        ["Cộng đồng", "/community"],
    ])("link điều hướng %s trỏ tới %s", (name, href) => {
        render(<MarketingHeader />);

        const nav = screen.getByRole("navigation", { name: "Giới thiệu" });

        expect(within(nav).getByRole("link", { name })).toHaveAttribute("href", href);
    });

    it("có nút đăng nhập và đăng ký", () => {
        render(<MarketingHeader />);

        expect(screen.getByRole("link", { name: "Đăng nhập" })).toHaveAttribute("href", "/login");
        expect(screen.getByRole("link", { name: "Bắt đầu miễn phí" })).toHaveAttribute(
            "href",
            "/register",
        );
    });
});
