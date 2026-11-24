import type { ReportReason, ReportStatus, ReportTarget } from "./types";

export const REPORT_STATUSES: readonly ReportStatus[] = ["OPEN", "RESOLVED", "DISMISSED"];

export const REPORT_STATUS_LABELS: Record<ReportStatus, string> = {
    OPEN: "Chờ xử lý",
    RESOLVED: "Đã gỡ",
    DISMISSED: "Đã bỏ qua",
};

export const REPORT_TARGET_LABELS: Record<ReportTarget, string> = {
    POST: "Bài viết",
    COMMENT: "Bình luận",
    USER: "Người dùng",
};

export const REPORT_REASON_LABELS: Record<ReportReason, string> = {
    SPAM: "Spam",
    OFFENSIVE: "Xúc phạm",
    CONTEST_SPOILER: "Lộ đề contest",
    PLAGIARISM: "Đạo nội dung",
    OTHER: "Khác",
};
