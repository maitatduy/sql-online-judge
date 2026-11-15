import { screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { renderWithProviders } from "@/test/render";

import { CourseOverview } from "./CourseOverview";

describe("CourseOverview", () => {
    it("hiện khoá học, số liệu ngắn và nút bắt đầu dẫn tới bài đầu tiên", async () => {
        renderWithProviders(<CourseOverview slug="sql-co-ban" />);

        expect(
            await screen.findByRole("heading", { level: 1, name: "SQL cơ bản" }),
        ).toBeInTheDocument();
        expect(screen.getByText("Dễ")).toHaveClass("text-difficulty-easy");
        expect(screen.getByText("2 chương")).toBeInTheDocument();
        expect(screen.getByText("4 bài")).toBeInTheDocument();
        expect(screen.getByText("40 phút")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Bắt đầu học" })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban/select-la-gi",
        );
    });

    it("liệt kê chương và bài theo thứ tự, mỗi bài dẫn tới trang bài giảng", async () => {
        renderWithProviders(<CourseOverview slug="sql-co-ban" />);

        const outline = within(await screen.findByRole("list", { name: "Đề cương" }));
        const chapters = outline.getAllByRole("heading", { level: 2 });
        expect(chapters.map((chapter) => chapter.textContent)).toEqual([
            "Làm quen với SELECT",
            "Lọc và sắp xếp",
        ]);
        const lessonLinks = outline.getAllByRole("link");
        expect(lessonLinks.map((link) => link.textContent)).toEqual([
            "1SELECT là gì",
            "2Chọn cột và đặt bí danh",
            "3Lọc dòng với WHERE",
            "4Sắp xếp với ORDER BY",
        ]);
        expect(lessonLinks[2]).toHaveAttribute("href", "/learn/sql-co-ban/menh-de-where");
    });

    it("báo không tìm thấy khi khoá học không tồn tại hoặc chưa xuất bản", async () => {
        renderWithProviders(<CourseOverview slug="khong-co" />);

        expect(await screen.findByText("Không tìm thấy khoá học")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Xem khoá học khác" })).toHaveAttribute(
            "href",
            "/learn",
        );
    });
});
