import type { QuizQuestionRequest } from "./types";

export const MIN_QUIZ_OPTIONS = 2;
export const MAX_QUIZ_OPTIONS = 6;
const MAX_PROMPT_LENGTH = 500;
const MAX_OPTION_LENGTH = 300;

export type QuizProblem = { field: "prompt" | "options"; message: string };

export function quizProblemOf(draft: QuizQuestionRequest): QuizProblem | null {
    if (draft.prompt.trim() === "") {
        return { field: "prompt", message: "Không được để trống" };
    }
    if (draft.prompt.length > MAX_PROMPT_LENGTH) {
        return { field: "prompt", message: "Tối đa 500 ký tự" };
    }
    if (draft.options.some((option) => option.label.trim() === "")) {
        return { field: "options", message: "Lựa chọn không được để trống" };
    }
    if (draft.options.some((option) => option.label.length > MAX_OPTION_LENGTH)) {
        return { field: "options", message: "Mỗi lựa chọn tối đa 300 ký tự" };
    }
    const correctCount = draft.options.filter((option) => option.correct).length;
    if (correctCount === 0) {
        return { field: "options", message: "Cần ít nhất một đáp án đúng" };
    }
    if (!draft.multiple && correctCount > 1) {
        return { field: "options", message: "Câu hỏi một đáp án chỉ có một lựa chọn đúng" };
    }
    return null;
}
