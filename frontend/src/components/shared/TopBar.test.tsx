import { screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "@/test/render";

import { TopBar } from "./TopBar";

vi.mock("next/navigation", () => ({
    usePathname: () => "/u/minh.anh",
}));

describe("TopBar", () => {
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
    });

    it("có ô tìm kiếm", () => {
        renderWithProviders(<TopBar />);

        expect(screen.getByRole("searchbox", { name: "Tìm kiếm" })).toHaveAttribute(
            "placeholder",
            "Tìm bài, contest, người dùng…",
        );
    });

    it("có nút thông báo và lối vào trợ lý AI", () => {
        renderWithProviders(<TopBar />);

        expect(screen.getByRole("button", { name: "Thông báo" })).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Trợ lý AI" })).toHaveAttribute("href", "/ai");
    });
});
