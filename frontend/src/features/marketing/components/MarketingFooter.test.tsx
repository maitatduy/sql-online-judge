import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MarketingFooter } from "./MarketingFooter";

describe("MarketingFooter", () => {
    it.each([
        ["Học tập", "Khoá học", "/learn"],
        ["Học tập", "Bài tập", "/problems"],
        ["Học tập", "Trợ lý AI", "/ai"],
        ["Thi đấu", "Contest", "/contests"],
        ["Thi đấu", "Bảng xếp hạng", "/leaderboard"],
        ["Cộng đồng", "Bài viết", "/community"],
        ["Cộng đồng", "Viết bài mới", "/community/new"],
        ["Tài khoản", "Đăng ký", "/register"],
        ["Tài khoản", "Cài đặt", "/settings"],
    ])("nhóm %s có link %s trỏ tới %s", (group, name, href) => {
        render(<MarketingFooter />);

        const nav = screen.getByRole("navigation", { name: group });

        expect(within(nav).getByRole("link", { name })).toHaveAttribute("href", href);
    });

    it("nhóm chủ đề dẫn tới danh sách bài đã lọc theo từng chủ đề của lộ trình", () => {
        render(<MarketingFooter />);

        const links = within(screen.getByRole("navigation", { name: "Chủ đề" })).getAllByRole(
            "link",
        );

        expect(links).toHaveLength(6);
        expect(links[1]).toHaveTextContent("Nối bảng");
        expect(links[1]).toHaveAttribute("href", "/problems?tag=join");
        expect(links[5]).toHaveAttribute("href", "/problems?tag=window-function");
    });

    it("dòng cuối ghi năm, tên trang và các dialect được chấm", () => {
        render(<MarketingFooter />);

        expect(
            screen.getByText(`© ${new Date().getFullYear()} SQL Online Judge`),
        ).toBeInTheDocument();
        expect(screen.getByText("Chấm trên PostgreSQL, MySQL, SQLite")).toBeInTheDocument();
    });
});
