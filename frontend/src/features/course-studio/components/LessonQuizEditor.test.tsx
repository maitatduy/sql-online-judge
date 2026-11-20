import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { LessonEditor } from "./LessonEditor";

const DRAFT_COURSE_ID = "0192f3f1-0000-7000-8000-000000000001";
const DRAFT_LESSON_ID = "0192f3f3-0000-7000-8000-000000000001";
const IN_REVIEW_COURSE_ID = "0192f3f1-0000-7000-8000-000000000002";
const IN_REVIEW_LESSON_ID = "0192f3f3-0000-7000-8000-000000000005";

async function renderQuiz(courseId = DRAFT_COURSE_ID, lessonId = DRAFT_LESSON_ID) {
    renderWithProviders(
        <ToastProvider>
            <LessonEditor courseId={courseId} lessonId={lessonId} />
        </ToastProvider>,
    );
    return within(await screen.findByRole("region", { name: "Quiz" }));
}

type Scope = ReturnType<typeof within>;

function fillQuestion(quiz: Scope, prompt: string, options: [string, boolean][]) {
    fireEvent.change(quiz.getByRole("textbox", { name: "Câu hỏi" }), {
        target: { value: prompt },
    });
    options.forEach(([label, correct], index) => {
        if (index >= 2) {
            fireEvent.click(quiz.getByRole("button", { name: "Thêm lựa chọn" }));
        }
        fireEvent.change(quiz.getByRole("textbox", { name: `Lựa chọn ${index + 1}` }), {
            target: { value: label },
        });
        const correctBox = quiz.getByRole("checkbox", { name: `Lựa chọn ${index + 1} đúng` });
        if ((correctBox as HTMLInputElement).checked !== correct) {
            fireEvent.click(correctBox);
        }
    });
}

describe("soạn quiz trong trình soạn bài giảng", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("liệt kê câu hỏi, đánh dấu lựa chọn đúng", async () => {
        const quiz = await renderQuiz();

        const question = within(await quiz.findByRole("group", { name: "Câu 1" }));
        expect(question.getByText("ROW_NUMBER() trả về gì?")).toBeInTheDocument();
        expect(question.getByText("Số thứ tự của dòng trong nhóm").closest("li")).toHaveTextContent(
            "Đúng",
        );
    });

    it("thêm câu hỏi nhiều đáp án với ba lựa chọn", async () => {
        const quiz = await renderQuiz();

        fireEvent.click(await quiz.findByRole("button", { name: "Thêm câu hỏi" }));
        fillQuestion(quiz, "Hàm nào là window function?", [
            ["RANK", true],
            ["LAG", true],
            ["COUNT DISTINCT", false],
        ]);
        fireEvent.click(quiz.getByRole("checkbox", { name: "Cho phép chọn nhiều đáp án" }));
        fireEvent.click(quiz.getByRole("button", { name: "Lưu câu hỏi" }));

        const added = within(await quiz.findByRole("group", { name: "Câu 2" }));
        expect(added.getByText("Hàm nào là window function?")).toBeInTheDocument();
        expect(added.getByText("Nhiều đáp án")).toBeInTheDocument();
        expect(added.getAllByText("Đúng")).toHaveLength(2);
    });

    it("báo lỗi khi chưa có đáp án đúng hoặc câu một đáp án có hai lựa chọn đúng", async () => {
        const quiz = await renderQuiz();

        fireEvent.click(await quiz.findByRole("button", { name: "Thêm câu hỏi" }));
        fillQuestion(quiz, "Câu thử", [
            ["A", false],
            ["B", false],
        ]);
        fireEvent.click(quiz.getByRole("button", { name: "Lưu câu hỏi" }));
        expect(await quiz.findByText("Cần ít nhất một đáp án đúng")).toBeInTheDocument();

        fillQuestion(quiz, "Câu thử", [
            ["A", true],
            ["B", true],
        ]);
        fireEvent.click(quiz.getByRole("button", { name: "Lưu câu hỏi" }));
        expect(
            await quiz.findByText("Câu hỏi một đáp án chỉ có một lựa chọn đúng"),
        ).toBeInTheDocument();
    });

    it("sửa câu hỏi rồi xoá sau khi xác nhận", async () => {
        const quiz = await renderQuiz();

        fireEvent.click(await quiz.findByRole("button", { name: "Sửa câu 1" }));
        fireEvent.change(quiz.getByRole("textbox", { name: "Câu hỏi" }), {
            target: { value: "ROW_NUMBER() đánh số theo gì?" },
        });
        fireEvent.click(quiz.getByRole("button", { name: "Lưu câu hỏi" }));
        expect(await quiz.findByText("ROW_NUMBER() đánh số theo gì?")).toBeInTheDocument();

        fireEvent.click(quiz.getByRole("button", { name: "Xoá câu 1" }));
        fireEvent.click(
            within(await screen.findByRole("alertdialog")).getByRole("button", {
                name: "Xoá câu hỏi",
            }),
        );
        expect(await quiz.findByText("Bài giảng chưa có câu hỏi nào.")).toBeInTheDocument();
    });

    it("khoá đang chờ duyệt chỉ hiện quiz, không có nút sửa", async () => {
        const quiz = await renderQuiz(IN_REVIEW_COURSE_ID, IN_REVIEW_LESSON_ID);

        await quiz.findByText("Bài giảng chưa có câu hỏi nào.");
        expect(quiz.queryByRole("button", { name: "Thêm câu hỏi" })).not.toBeInTheDocument();
    });
});
