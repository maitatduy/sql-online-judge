import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import NotFound from "./not-found";

describe("NotFound", () => {
    it("báo không tìm thấy trang bằng tiếng Việt và có link về trang chủ", () => {
        render(<NotFound />);

        expect(
            screen.getByRole("heading", { level: 1, name: "Không tìm thấy trang" }),
        ).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
    });

    it("logo dẫn về trang chủ", () => {
        render(<NotFound />);

        expect(screen.getByRole("link", { name: "SQL Online Judge" })).toHaveAttribute("href", "/");
    });

    it("minh hoạ bằng một truy vấn tìm trang không trả về dòng nào", () => {
        render(<NotFound />);

        const example = screen.getByRole("figure", {
            name: "Truy vấn tìm trang không trả về dòng nào",
        });

        expect(within(example).getByText("WA · 404")).toBeInTheDocument();
        expect(within(example).getByText("0 dòng")).toBeInTheDocument();
    });
});
