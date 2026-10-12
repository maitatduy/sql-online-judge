import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { HeroSection } from "./HeroSection";

describe("HeroSection", () => {
    it("có tiêu đề chính của trang", () => {
        render(<HeroSection />);

        expect(
            screen.getByRole("heading", { level: 1, name: "Học SQL bằng cách viết SQL." }),
        ).toBeInTheDocument();
    });

    it("có hai nút kêu gọi hành động trỏ đúng đích", () => {
        render(<HeroSection />);

        expect(screen.getByRole("link", { name: "Bắt đầu miễn phí" })).toHaveAttribute(
            "href",
            "/register",
        );
        expect(screen.getByRole("link", { name: "Xem bài tập" })).toHaveAttribute(
            "href",
            "/problems",
        );
    });

    it("bảng kết quả minh hoạ có cột khớp với câu truy vấn", () => {
        render(<HeroSection />);

        const headers = screen.getAllByRole("columnheader").map((header) => header.textContent);

        expect(headers).toEqual(["department", "name", "salary"]);
    });
});
