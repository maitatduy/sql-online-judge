import type { PostCategory, PostTab, ReportReason } from "./types";

export const POST_CATEGORIES: readonly PostCategory[] = [
    "QUESTION",
    "SOLUTION",
    "INTERVIEW",
    "BLOG",
    "FEEDBACK",
];

export const POST_CATEGORY_LABELS: Record<PostCategory, string> = {
    QUESTION: "Hỏi đáp",
    SOLUTION: "Chia sẻ lời giải",
    INTERVIEW: "Phỏng vấn",
    BLOG: "Blog",
    FEEDBACK: "Góp ý",
};

export const POST_CATEGORY_CLASSES: Record<PostCategory, string> = {
    QUESTION: "bg-post-question/12 text-post-question",
    SOLUTION: "bg-post-solution/12 text-post-solution",
    INTERVIEW: "bg-post-interview/12 text-post-interview",
    BLOG: "bg-post-blog/12 text-post-blog",
    FEEDBACK: "bg-post-feedback/12 text-post-feedback",
};

export const POST_TABS: readonly PostTab[] = ["HOT", "NEW", "UNANSWERED", "SAVED"];

export const POST_TAB_LABELS: Record<PostTab, string> = {
    HOT: "Nổi bật",
    NEW: "Mới nhất",
    UNANSWERED: "Chưa có trả lời",
    SAVED: "Đã lưu",
};

export const REPORT_REASONS: readonly ReportReason[] = [
    "SPAM",
    "OFFENSIVE",
    "CONTEST_SPOILER",
    "PLAGIARISM",
    "OTHER",
];

export const REPORT_REASON_LABELS: Record<ReportReason, string> = {
    SPAM: "Spam, quảng cáo",
    OFFENSIVE: "Ngôn từ xúc phạm",
    CONTEST_SPOILER: "Lộ đáp án contest",
    PLAGIARISM: "Sao chép lời giải",
    OTHER: "Lý do khác",
};
