import { screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { clearMockSession, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { LessonReader } from "./LessonReader";

const replaceRoute = vi.fn();

vi.mock("next/navigation", () => ({
    usePathname: () => "/learn/sql-co-ban/select-la-gi",
    useRouter: () => ({ replace: replaceRoute }),
}));

function renderLesson(lessonSlug: string) {
    return renderWithProviders(<LessonReader courseSlug="sql-co-ban" lessonSlug={lessonSlug} />);
}

describe("LessonReader", () => {
    beforeEach(() => {
        localStorage.clear();
        replaceRoute.mockClear();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("hiện tên bài, chương và nội dung markdown có khối SQL", async () => {
        renderLesson("chon-cot");

        expect(
            await screen.findByRole("heading", { level: 1, name: "Chọn cột và đặt bí danh" }),
        ).toBeInTheDocument();
        expect(
            within(screen.getByRole("main")).getByText("Làm quen với SELECT"),
        ).toBeInTheDocument();
        expect(screen.getByText("SELECT name, salary FROM employees;")).toBeInTheDocument();
    });

    it("đánh dấu bài đang đọc trong đề cương", async () => {
        renderLesson("chon-cot");

        const outline = within(await screen.findByRole("navigation", { name: "Đề cương" }));
        expect(outline.getByRole("link", { name: /Chọn cột và đặt bí danh/ })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(outline.getByRole("link", { name: /SELECT là gì/ })).not.toHaveAttribute(
            "aria-current",
        );
    });

    it("có nút sang bài sau và không có bài trước ở bài đầu tiên", async () => {
        renderLesson("select-la-gi");

        expect(await screen.findByRole("link", { name: /Bài sau/ })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban/chon-cot",
        );
        expect(screen.queryByRole("link", { name: /Bài trước/ })).not.toBeInTheDocument();
    });

    it("bài cuối dẫn ngược về bài trước, sang chương khác vẫn nối liền", async () => {
        renderLesson("order-by");

        expect(await screen.findByRole("link", { name: /Bài trước/ })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban/menh-de-where",
        );
        expect(screen.queryByRole("link", { name: /Bài sau/ })).not.toBeInTheDocument();
    });

    it("chuyển sang trang đăng nhập khi chưa đăng nhập", async () => {
        clearMockSession();
        renderLesson("select-la-gi");

        await waitFor(() =>
            expect(replaceRoute).toHaveBeenCalledWith(
                "/login?next=%2Flearn%2Fsql-co-ban%2Fselect-la-gi",
            ),
        );
    });

    it("báo không tìm thấy bài và dẫn về trang khoá", async () => {
        renderLesson("khong-co");

        expect(await screen.findByText("Không tìm thấy bài giảng")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Về khoá học" })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban",
        );
    });
});
