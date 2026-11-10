import type { AnswerCheck, ProblemAnswer, StudioProblem } from "./types";

export interface PublishCondition {
    label: string;
    isMet: boolean;
    detail: string;
    action: { label: string; step: string };
}

function answerCondition(answer: ProblemAnswer | null | undefined): PublishCondition {
    const isMet =
        answer !== null &&
        answer !== undefined &&
        answer.solutions.length > 0 &&
        answer.testcases.length > 0;
    return {
        label: "Đáp án và testcase",
        isMet,
        detail: isMet
            ? `${answer.solutions.length} đáp án, ${answer.testcases.length} testcase`
            : "Chưa có đáp án chuẩn hoặc testcase",
        action: { label: "Thêm đáp án", step: "/answer" },
    };
}

function checkDetail(check: AnswerCheck | null | undefined): string {
    if (check === undefined) {
        return "Đang tải kết quả kiểm tra";
    }
    if (check === null) {
        return "Chưa kiểm tra đáp án";
    }
    if (!check.isCurrent) {
        return "Đáp án đã sửa sau lần kiểm tra gần nhất";
    }
    if (check.status === "PENDING") {
        return "Đang kiểm tra";
    }
    return check.status === "PASSED" ? "Đạt trên mọi dialect" : "Chưa đạt kiểm tra";
}

export function publishConditionsOf(
    problem: StudioProblem,
    answer: ProblemAnswer | null | undefined,
    check: AnswerCheck | null | undefined,
): PublishCondition[] {
    const hasStatement = problem.statementMarkdown.trim() !== "";
    return [
        {
            label: "Đề bài",
            isMet: hasStatement,
            detail: hasStatement ? "Đã có đề bài" : "Chưa có đề bài",
            action: { label: "Soạn đề bài", step: "/statement" },
        },
        {
            label: "Database",
            isMet: problem.dataset !== null,
            detail: problem.dataset
                ? `Dùng database ${problem.dataset.name}`
                : "Chưa chọn database",
            action: { label: "Chọn database", step: "/database" },
        },
        answerCondition(answer),
        {
            label: "Kiểm tra đáp án",
            isMet: check?.status === "PASSED" && check.isCurrent,
            detail: checkDetail(check),
            action: { label: "Chạy kiểm tra", step: "/answer" },
        },
    ];
}
