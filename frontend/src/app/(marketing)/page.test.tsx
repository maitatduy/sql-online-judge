import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { renderWithProviders } from "@/test/render";

import LandingPage, { metadata } from "./page";

describe("LandingPage", () => {
    it("vùng nội dung chính là đích của link bỏ qua", () => {
        renderWithProviders(<LandingPage />);

        expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
    });

    it("khai báo Open Graph tiếng Việt trùng với tiêu đề và mô tả của trang", () => {
        expect(metadata.openGraph).toMatchObject({
            title: "SQL Online Judge - Học SQL bằng cách viết SQL",
            siteName: "SQL Online Judge",
            description: metadata.description,
            locale: "vi_VN",
            type: "website",
        });
    });

    it("có đúng một tiêu đề cấp 1", () => {
        renderWithProviders(<LandingPage />);

        const headings = screen.getAllByRole("heading", { level: 1 });

        expect(headings).toHaveLength(1);
        expect(headings[0]).toHaveTextContent("Học SQL bằng cách viết SQL.");
    });

    it("hiện dải số liệu lấy từ API", async () => {
        renderWithProviders(<LandingPage />);

        expect(await screen.findByText("bài tập có máy chấm")).toBeInTheDocument();
    });

    it("hiện các khối nội dung theo thứ tự, không còn khối trụ cột và trợ lý AI", () => {
        renderWithProviders(<LandingPage />);

        const sectionTitles = screen
            .getAllByRole("heading", { level: 2 })
            .map((heading) => heading.textContent);

        expect(sectionTitles).toEqual([
            "Lộ trình SQL",
            "Contest sắp tới",
            "Top rating",
            "Database đã sẵn sàng, chỉ còn thiếu câu truy vấn của bạn.",
        ]);
    });
});
