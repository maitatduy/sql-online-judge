import type { ProblemVisibility } from "../problem-reviews/types";

export const VISIBILITIES: readonly ProblemVisibility[] = ["PUBLIC", "CONTEST_ONLY", "HIDDEN"];

export const VISIBILITY_LABELS: Record<ProblemVisibility, string> = {
    PUBLIC: "Công khai",
    CONTEST_ONLY: "Chỉ trong contest",
    HIDDEN: "Đang ẩn",
};

export const VISIBILITY_CLASSES: Record<ProblemVisibility, string> = {
    PUBLIC: "bg-visibility-public/12 text-visibility-public",
    CONTEST_ONLY: "bg-visibility-contest/12 text-visibility-contest",
    HIDDEN: "bg-visibility-hidden/12 text-visibility-hidden",
};
