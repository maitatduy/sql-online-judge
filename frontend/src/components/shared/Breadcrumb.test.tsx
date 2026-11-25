import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Breadcrumb } from "./Breadcrumb";

describe("Breadcrumb", () => {
    it("các mục trước là link, mục cuối là trang hiện tại", () => {
        render(
            <Breadcrumb
                items={[
                    { label: "Trang chủ", href: "/" },
                    { label: "Luyện tập", href: "/problems" },
                    { label: "#1. Tổng lương" },
                ]}
            />,
        );

        const nav = screen.getByRole("navigation", { name: "Đường dẫn" });
        expect(within(nav).getAllByRole("listitem")).toHaveLength(3);
        expect(within(nav).getByRole("link", { name: "Trang chủ" })).toHaveAttribute("href", "/");
        expect(within(nav).getByRole("link", { name: "Luyện tập" })).toHaveAttribute(
            "href",
            "/problems",
        );
        expect(within(nav).getByText("#1. Tổng lương")).toHaveAttribute("aria-current", "page");
        expect(within(nav).queryByRole("link", { name: "#1. Tổng lương" })).not.toBeInTheDocument();
    });
});
