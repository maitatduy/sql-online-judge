import { screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { completeMockLesson, resetMockLearningProgress } from "@/mocks/learning-progress";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { CourseOverview } from "./CourseOverview";

const FIRST_LESSON_KEY = "01a1d2c0-0000-7000-8000-000000000201";
const ALL_LESSON_KEYS = [
    FIRST_LESSON_KEY,
    "01a1d2c0-0000-7000-8000-000000000202",
    "01a1d2c0-0000-7000-8000-000000000203",
    "01a1d2c0-0000-7000-8000-000000000204",
];

describe("CourseOverview", () => {
    beforeEach(() => {
        localStorage.clear();
        resetMockLearningProgress();
    });

    it("người đang học thấy tiến độ, nút học tiếp và dấu đã hoàn thành trên đề cương", async () => {
        writeMockSession(DEMO_SESSION_USER);
        completeMockLesson("sql-co-ban", FIRST_LESSON_KEY);
        renderWithProviders(<CourseOverview slug="sql-co-ban" />);

        expect(await screen.findByRole("link", { name: "Học tiếp" })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban/chon-cot",
        );
        expect(screen.getByText("1/4 bài")).toBeInTheDocument();
        expect(screen.getByRole("progressbar", { name: "Tiến độ khoá học" })).toHaveAttribute(
            "aria-valuenow",
            "25",
        );
        const outline = within(screen.getByRole("list", { name: "Đề cương" }));
        expect(
            outline.getByRole("link", { name: /SELECT là gì.*Đã hoàn thành/ }),
        ).toBeInTheDocument();
        expect(
            outline.getByRole("link", { name: /Chọn cột và đặt bí danh/ }).textContent,
        ).not.toContain("Đã hoàn thành");
    });

    it("học xong cả khoá thì nút chuyển thành ôn lại từ bài đầu", async () => {
        writeMockSession(DEMO_SESSION_USER);
        ALL_LESSON_KEYS.forEach((lessonKey) => completeMockLesson("sql-co-ban", lessonKey));
        renderWithProviders(<CourseOverview slug="sql-co-ban" />);

        expect(await screen.findByRole("link", { name: "Ôn lại" })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban/select-la-gi",
        );
        expect(screen.getByText("4/4 bài")).toBeInTheDocument();
    });

    it("hiện khoá học, số liệu ngắn và nút bắt đầu dẫn tới bài đầu tiên", async () => {
        renderWithProviders(<CourseOverview slug="sql-co-ban" />);

        expect(
            await screen.findByRole("heading", { level: 1, name: "SQL cơ bản" }),
        ).toBeInTheDocument();
        const facts = within(screen.getByRole("list", { name: "Thông tin khoá học" }));
        expect(facts.getByText("Dễ")).toHaveClass("text-difficulty-easy");
        expect(facts.getByText("2 chương")).toBeInTheDocument();
        expect(facts.getByText("4 bài")).toBeInTheDocument();
        expect(facts.getByText("40 phút")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Bắt đầu học" })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban/select-la-gi",
        );
    });

    it("hiện câu giới thiệu và các ý sẽ học dạng gạch đầu dòng", async () => {
        renderWithProviders(<CourseOverview slug="sql-co-ban" />);

        expect(await screen.findByText("Chọn cột, lọc dòng và sắp xếp kết quả.")).toBeVisible();
        const points = within(screen.getByRole("list", { name: "Bạn sẽ học" }));
        expect(points.getAllByRole("listitem").map((item) => item.textContent)).toEqual([
            "Đọc dữ liệu bằng SELECT",
            "Lọc dòng với WHERE",
            "Sắp xếp với ORDER BY",
        ]);
    });

    it("có breadcrumb về trang chủ và danh mục khoá học", async () => {
        renderWithProviders(<CourseOverview slug="sql-co-ban" />);

        const breadcrumb = within(await screen.findByRole("navigation", { name: "Đường dẫn" }));
        expect(breadcrumb.getByRole("link", { name: "Trang chủ" })).toHaveAttribute("href", "/");
        expect(breadcrumb.getByRole("link", { name: "Khoá học" })).toHaveAttribute(
            "href",
            "/learn",
        );
        expect(breadcrumb.getByText("SQL cơ bản")).toHaveAttribute("aria-current", "page");
    });

    it("liệt kê chương và bài theo thứ tự, mỗi bài dẫn tới trang bài giảng", async () => {
        renderWithProviders(<CourseOverview slug="sql-co-ban" />);

        const outline = within(await screen.findByRole("list", { name: "Đề cương" }));
        const chapters = outline.getAllByRole("heading", { level: 2 });
        expect(chapters.map((chapter) => chapter.textContent)).toEqual([
            "Chương 1: Làm quen với SELECT",
            "Chương 2: Lọc và sắp xếp",
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
