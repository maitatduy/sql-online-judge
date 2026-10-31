import type { ProblemStatus } from "./types";

export const PROBLEM_STATUS_LABELS: Record<ProblemStatus, string> = {
    DRAFT: "Nháp",
    PENDING_REVIEW: "Chờ duyệt",
    CHANGES_REQUESTED: "Cần sửa",
    REJECTED: "Bị từ chối",
    PUBLISHED: "Đã xuất bản",
};

export const LOCKED_MESSAGES: Partial<Record<ProblemStatus, string>> = {
    PENDING_REVIEW: "Bài đang chờ duyệt nên chưa sửa được.",
    REJECTED: "Bài đã bị từ chối nên không sửa được.",
    PUBLISHED: "Bài đã xuất bản nên chưa sửa được.",
};

export const STATUS_TABS: readonly ProblemStatus[] = [
    "DRAFT",
    "PENDING_REVIEW",
    "CHANGES_REQUESTED",
    "PUBLISHED",
];
