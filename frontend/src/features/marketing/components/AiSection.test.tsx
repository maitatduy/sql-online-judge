import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AiSection } from "./AiSection";

describe("AiSection", () => {
    it("có tiêu đề khối", () => {
        render(<AiSection />);

        expect(
            screen.getByRole("heading", { level: 2, name: "Gợi ý, không giải hộ." }),
        ).toBeInTheDocument();
    });

    it("liệt kê bốn khả năng của trợ lý", () => {
        render(<AiSection />);

        const features = within(screen.getByRole("list")).getAllByRole("listitem");

        expect(features.map((feature) => feature.textContent)).toEqual([
            "Giải thích lỗi cú pháp bằng tiếng Việt",
            "Phân tích EXPLAIN, gợi ý tối ưu",
            "Sinh bài luyện theo điểm yếu của bạn",
            "Tự tắt trong giờ thi contest",
        ]);
    });

    it("có đoạn hội thoại mẫu ghi rõ ai nói", () => {
        render(<AiSection />);

        const chat = screen.getByRole("figure", { name: "Ví dụ hội thoại với trợ lý AI" });

        expect(within(chat).getByText("À, phải dùng LEFT JOIN!")).toBeInTheDocument();
        expect(within(chat).getAllByText("Bạn:")).toHaveLength(2);
        expect(within(chat).getAllByText("Trợ lý AI:")).toHaveLength(2);
    });
});
