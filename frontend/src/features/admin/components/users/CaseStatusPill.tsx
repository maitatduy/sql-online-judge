import type { ViolationStatus } from "@/features/admin/users/types";
import { cn } from "@/lib/utils";

const STATUS_APPEARANCES: Record<ViolationStatus, { label: string; className: string }> = {
    OPEN: { label: "Chờ xử lý", className: "bg-case-open/12 text-case-open" },
    RESOLVED: { label: "Đã xử lý", className: "bg-case-resolved/12 text-case-resolved" },
};

interface CaseStatusPillProps {
    status: ViolationStatus;
}

export function CaseStatusPill({ status }: CaseStatusPillProps) {
    const { label, className } = STATUS_APPEARANCES[status];

    return (
        <span
            className={cn(
                "inline-flex h-5.5 shrink-0 items-center rounded-full px-2 text-xs font-semibold whitespace-nowrap",
                className,
            )}
        >
            {label}
        </span>
    );
}
