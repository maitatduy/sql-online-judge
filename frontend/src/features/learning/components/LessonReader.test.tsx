import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { completeMockLesson, resetMockLearningProgress } from "@/mocks/learning-progress";
import { clearMockSession, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { LessonReader } from "./LessonReader";

const replaceRoute = vi.fn();

vi.mock("@/components/shared/LazySqlEditor", () => ({
    LazySqlEditor: ({ value, ariaLabel }: { value: string; ariaLabel: string }) => (
        <textarea aria-label={ariaLabel} value={value} readOnly />
    ),
}));

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
        resetMockLearningProgress();
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

    it("đề cương là menu nhiều cấp: chương đang học mở sẵn, chương khác đóng và mở được", async () => {
        renderLesson("chon-cot");

        const outline = within(await screen.findByRole("navigation", { name: "Đề cương" }));
        const current = outline.getByRole("button", { name: "Chương 1: Làm quen với SELECT" });
        const other = outline.getByRole("button", { name: "Chương 2: Lọc và sắp xếp" });
        expect(current).toHaveAttribute("aria-expanded", "true");
        expect(other).toHaveAttribute("aria-expanded", "false");
        expect(outline.queryByRole("link", { name: /Lọc dòng với WHERE/ })).not.toBeInTheDocument();

        fireEvent.click(other);

        expect(other).toHaveAttribute("aria-expanded", "true");
        expect(outline.getByRole("link", { name: /Lọc dòng với WHERE/ })).toBeInTheDocument();
        fireEvent.click(current);
        expect(outline.queryByRole("link", { name: /SELECT là gì/ })).not.toBeInTheDocument();
        expect(outline.queryByText(/\d+\/\d+ bài/)).not.toBeInTheDocument();
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

    it("bấm hoàn thành bài thì hiện đã hoàn thành và đánh dấu trên đề cương", async () => {
        renderLesson("chon-cot");

        fireEvent.click(await screen.findByRole("button", { name: "Hoàn thành bài học" }));

        expect(
            await within(screen.getByRole("main")).findByText("Đã hoàn thành"),
        ).toBeInTheDocument();
        expect(
            screen.queryByRole("button", { name: "Hoàn thành bài học" }),
        ).not.toBeInTheDocument();
        const outline = within(screen.getByRole("navigation", { name: "Đề cương" }));
        expect(
            outline.getByRole("link", { name: /Đã hoàn thànhs*Chọn cột và đặt bí danh/ }),
        ).toBeInTheDocument();
    });

    it("bài đã hoàn thành từ trước không còn nút hoàn thành", async () => {
        completeMockLesson("sql-co-ban", "01a1d2c0-0000-7000-8000-000000000201");
        renderLesson("select-la-gi");

        expect(
            await within(screen.getByRole("main")).findByText("Đã hoàn thành"),
        ).toBeInTheDocument();
        expect(
            screen.queryByRole("button", { name: "Hoàn thành bài học" }),
        ).not.toBeInTheDocument();
    });

    it("hiện ô SQL của bài với câu mẫu", async () => {
        renderLesson("select-la-gi");

        const cell = within(await screen.findByRole("region", { name: "Ô SQL" }));
        expect(cell.getByRole("textbox", { name: "Câu SQL" })).toHaveValue(
            "SELECT id, name FROM departments;",
        );
        expect(cell.getByRole("button", { name: "Chạy" })).toBeInTheDocument();
    });

    it("bài có quiz hiện câu hỏi thay cho nút hoàn thành", async () => {
        renderLesson("select-la-gi");

        const quiz = within(await screen.findByRole("form", { name: "Câu hỏi" }));
        expect(quiz.getByText("Dấu * sau SELECT nghĩa là gì?")).toBeInTheDocument();
        expect(quiz.getAllByRole("radio")).toHaveLength(2);
        expect(
            screen.queryByRole("button", { name: "Hoàn thành bài học" }),
        ).not.toBeInTheDocument();
    });

    it("trả lời sai thì báo chưa đúng và bài chưa hoàn thành", async () => {
        renderLesson("select-la-gi");

        fireEvent.click(await screen.findByRole("radio", { name: "Lấy mọi bảng trong database" }));
        fireEvent.click(screen.getByRole("button", { name: "Nộp bài" }));

        expect(await screen.findByText("Chưa đúng")).toBeInTheDocument();
        expect(
            within(screen.getByRole("main")).queryByText("Đã hoàn thành"),
        ).not.toBeInTheDocument();
    });

    it("trả lời đúng hết thì bài được hoàn thành", async () => {
        renderLesson("select-la-gi");

        fireEvent.click(await screen.findByRole("radio", { name: "Lấy mọi cột của bảng" }));
        fireEvent.click(screen.getByRole("button", { name: "Nộp bài" }));

        expect(
            await within(screen.getByRole("main")).findByText("Đã hoàn thành"),
        ).toBeInTheDocument();
        const quiz = within(screen.getByRole("form", { name: "Câu hỏi" }));
        expect(quiz.getByRole("radio", { name: "Lấy mọi cột của bảng" })).toBeChecked();
        expect(quiz.getByText("Đúng")).toBeInTheDocument();
    });

    it("mở lại bài đã trả lời đúng vẫn thấy lựa chọn và kết quả", async () => {
        const first = renderLesson("select-la-gi");
        fireEvent.click(await screen.findByRole("radio", { name: "Lấy mọi cột của bảng" }));
        fireEvent.click(screen.getByRole("button", { name: "Nộp bài" }));
        await within(screen.getByRole("main")).findByText("Đã hoàn thành");
        first.unmount();

        renderLesson("select-la-gi");

        const quiz = within(await screen.findByRole("form", { name: "Câu hỏi" }));
        expect(quiz.getByRole("radio", { name: "Lấy mọi cột của bảng" })).toBeChecked();
        expect(quiz.getByText("Đúng")).toBeInTheDocument();
    });

    it("có breadcrumb tới trang chủ, danh mục và trang khoá học", async () => {
        renderLesson("select-la-gi");

        const breadcrumb = within(screen.getByRole("navigation", { name: "Đường dẫn" }));
        expect(await breadcrumb.findByRole("link", { name: "SQL cơ bản" })).toHaveAttribute(
            "href",
            "/learn/sql-co-ban",
        );
        expect(breadcrumb.getByRole("link", { name: "Khoá học" })).toHaveAttribute(
            "href",
            "/learn",
        );
        expect(breadcrumb.getByText("SELECT là gì")).toHaveAttribute("aria-current", "page");
    });
});
