import type { ReviewAnswerCheck, ReviewDecision, ReviewProblemStatus } from "./types";

export const REVIEW_STATUS_LABELS: Record<ReviewProblemStatus, string> = {
    DRAFT: "Nháp",
    PENDING_REVIEW: "Chờ duyệt",
    CHANGES_REQUESTED: "Cần sửa",
    REJECTED: "Từ chối",
    PUBLISHED: "Đã xuất bản",
};

export const REVIEW_DECISION_LABELS: Record<ReviewDecision, string> = {
    APPROVED: "Đã duyệt",
    CHANGES_REQUESTED: "Yêu cầu sửa",
    REJECTED: "Từ chối",
};

export interface AnswerCheckSummary {
    text: string;
    canApprove: boolean;
}

export function answerCheckSummaryOf(
    check: ReviewAnswerCheck | null | undefined,
): AnswerCheckSummary {
    if (check === undefined) {
        return { text: "Đang tải kết quả kiểm tra đáp án.", canApprove: false };
    }
    if (check === null) {
        return { text: "Người ra đề chưa kiểm tra đáp án.", canApprove: false };
    }
    if (!check.isCurrent) {
        return { text: "Đáp án đã sửa sau lần kiểm tra gần nhất.", canApprove: false };
    }
    if (check.status === "PENDING") {
        return { text: "Đang kiểm tra đáp án.", canApprove: false };
    }
    const passed = check.testcases.filter((testcase) => testcase.isPassed).length;
    const counts = `${passed}/${check.testcaseCount} testcase khớp trên mọi dialect.`;
    return check.status === "PASSED"
        ? { text: `Đạt: ${counts}`, canApprove: true }
        : { text: `Chưa đạt: ${counts}`, canApprove: false };
}
