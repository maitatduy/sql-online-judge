import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { TopBarNav } from "./TopBarNav";

const navigation = vi.hoisted(() => ({ pathname: "/" }));

vi.mock("next/navigation", () => ({
    usePathname: () => navigation.pathname,
}));

describe("TopBarNav", () => {
    it.each([
        ["Học", "/learn"],
        ["Luyện tập", "/problems"],
        ["Contest", "/contests"],
        ["Cộng đồng", "/community"],
        ["Xếp hạng", "/leaderboard"],
    ])("link %s trỏ tới %s", (name, href) => {
        navigation.pathname = "/";
        render(<TopBarNav />);

        expect(screen.getByRole("link", { name })).toHaveAttribute("href", href);
    });

    it("đánh dấu mục đang mở, kể cả ở trang con", () => {
        navigation.pathname = "/problems/highest-salary";
        render(<TopBarNav />);

        expect(screen.getByRole("link", { name: "Luyện tập" })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(screen.getByRole("link", { name: "Học" })).not.toHaveAttribute("aria-current");
    });

    it("không đánh dấu mục nào ở trang ngoài điều hướng", () => {
        navigation.pathname = "/u/minh.anh";
        render(<TopBarNav />);

        screen.getAllByRole("link").forEach((link) => {
            expect(link).not.toHaveAttribute("aria-current");
        });
    });
});
