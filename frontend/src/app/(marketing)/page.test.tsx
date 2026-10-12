import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import LandingPage, { metadata } from "./page";

describe("LandingPage", () => {
    it("vùng nội dung chính là đích của link bỏ qua", () => {
        render(<LandingPage />);

        expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
    });

    it("khai báo Open Graph tiếng Việt trùng với tiêu đề và mô tả của trang", () => {
        expect(metadata.openGraph).toMatchObject({
            title: "SQL Judge - Học SQL bằng cách viết SQL",
            description: metadata.description,
            locale: "vi_VN",
            type: "website",
        });
    });

    it("có đúng một tiêu đề cấp 1", () => {
        render(<LandingPage />);

        const headings = screen.getAllByRole("heading", { level: 1 });

        expect(headings).toHaveLength(1);
        expect(headings[0]).toHaveTextContent("Học SQL bằng cách viết SQL.");
    });

    it("hiện dải số liệu", () => {
        render(<LandingPage />);

        expect(screen.getByText("bài tập có máy chấm")).toBeInTheDocument();
    });

    it("hiện các khối nội dung theo thứ tự của canvas", () => {
        render(<LandingPage />);

        const sectionTitles = screen
            .getAllByRole("heading", { level: 2 })
            .map((heading) => heading.textContent);

        expect(sectionTitles).toEqual([
            "Từ câu SELECT đầu tiên đến bảng xếp hạng contest",
            "Gợi ý, không giải hộ.",
            "Contest sắp tới",
            "Top rating",
            "Sáu chặng, đi theo tốc độ của bạn",
            "Câu truy vấn đầu tiên chỉ mất 30 giây.",
        ]);
    });
});
