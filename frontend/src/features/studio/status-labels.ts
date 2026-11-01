import type { ProblemStatus } from "./types";

export const LOCKED_MESSAGES: Partial<Record<ProblemStatus, string>> = {
    PENDING_REVIEW: "Bài đang chờ duyệt nên chưa sửa được.",
    REJECTED: "Bài đã bị từ chối nên không sửa được.",
    PUBLISHED: "Bài đã xuất bản nên chưa sửa được.",
};
