import type { CourseRevisionStatus } from "../types";

const LABELS: Record<CourseRevisionStatus, string> = {
    DRAFT: "Bản nháp",
    IN_REVIEW: "Chờ duyệt",
    PUBLISHED: "Đã xuất bản",
    SUPERSEDED: "Bản cũ",
};

interface RevisionStatusBadgeProps {
    status: CourseRevisionStatus;
}

export function RevisionStatusBadge({ status }: RevisionStatusBadgeProps) {
    return (
        <span className="inline-flex h-6 items-center rounded-full bg-surface-2 px-2.5 text-sm font-semibold text-muted-foreground">
            {LABELS[status]}
        </span>
    );
}
