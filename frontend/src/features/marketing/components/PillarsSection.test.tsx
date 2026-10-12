import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { PillarsSection } from "./PillarsSection";

describe("PillarsSection", () => {
    it("có tiêu đề khối", () => {
        render(<PillarsSection />);

        expect(
            screen.getByRole("heading", {
                level: 2,
                name: "Từ câu SELECT đầu tiên đến bảng xếp hạng contest",
            }),
        ).toBeInTheDocument();
    });

    it("có đủ bốn trụ cột theo thứ tự", () => {
        render(<PillarsSection />);

        const titles = screen
            .getAllByRole("heading", { level: 3 })
            .map((heading) => heading.textContent);

        expect(titles).toEqual(["Học", "Luyện", "Thi", "Cộng đồng"]);
    });

    it("thẻ Luyện minh hoạ bằng các verdict", () => {
        render(<PillarsSection />);

        expect(screen.getByText("AC")).toHaveAttribute("title", "Đúng");
        expect(screen.getByText("TLE")).toHaveAttribute("title", "Quá thời gian");
    });
});
