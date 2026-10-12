import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MarketingFooter } from "./MarketingFooter";

describe("MarketingFooter", () => {
    it.each([
        ["Lộ trình học", "/learn"],
        ["Bài tập", "/problems"],
        ["Contest", "/contests"],
        ["Diễn đàn", "/community"],
        ["Xếp hạng", "/leaderboard"],
    ])("link %s trỏ tới %s", (name, href) => {
        render(<MarketingFooter />);

        expect(screen.getByRole("link", { name })).toHaveAttribute("href", href);
    });

    it("không hiện link chưa có trang đích", () => {
        render(<MarketingFooter />);

        expect(screen.queryByRole("link", { name: "Điều khoản" })).not.toBeInTheDocument();
        expect(
            screen.queryByRole("link", { name: "Ra đề cùng chúng tôi" }),
        ).not.toBeInTheDocument();
    });
});
