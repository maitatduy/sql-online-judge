import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StudioNav } from "./StudioNav";

const pathname = vi.hoisted(() => ({ current: "/studio/datasets" }));

vi.mock("next/navigation", () => ({
    usePathname: () => pathname.current,
}));

describe("StudioNav", () => {
    it("có hai mục và đánh dấu mục đang mở", () => {
        render(<StudioNav />);

        const nav = within(screen.getByRole("navigation", { name: "Khu vực người ra đề" }));
        expect(nav.getByRole("link", { name: "Bài của tôi" })).toHaveAttribute(
            "href",
            "/studio/problems",
        );
        expect(nav.getByRole("link", { name: "Dataset của tôi" })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(nav.getByRole("link", { name: "Bài của tôi" })).not.toHaveAttribute("aria-current");
    });

    it("trang con của bài vẫn đánh dấu mục Bài của tôi", () => {
        pathname.current = "/studio/problems/0192f3d0-0000-7000-8000-000000000001/statement";
        render(<StudioNav />);

        expect(screen.getByRole("link", { name: "Bài của tôi" })).toHaveAttribute(
            "aria-current",
            "page",
        );
    });
});
