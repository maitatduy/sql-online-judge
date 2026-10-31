import type { ProblemStatus } from "./types";

export const PROBLEM_STATUS_LABELS: Record<ProblemStatus, string> = {
    DRAFT: "Nháp",
    PENDING_REVIEW: "Chờ duyệt",
    CHANGES_REQUESTED: "Cần sửa",
    REJECTED: "Bị từ chối",
    PUBLISHED: "Đã xuất bản",
};

export const STATUS_TABS: readonly ProblemStatus[] = [
    "DRAFT",
    "PENDING_REVIEW",
    "CHANGES_REQUESTED",
    "PUBLISHED",
];
