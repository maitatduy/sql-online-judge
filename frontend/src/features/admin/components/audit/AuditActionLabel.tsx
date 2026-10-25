import { AUDIT_ACTION_LABELS, isDestructiveAction } from "@/features/admin/audit/action-labels";
import type { AuditAction } from "@/features/admin/audit/types";
import { cn } from "@/lib/utils";

interface AuditActionLabelProps {
    action: AuditAction;
}

export function AuditActionLabel({ action }: AuditActionLabelProps) {
    return (
        <span
            className={cn(
                "inline-flex h-5.5 items-center rounded-full px-2 text-xs font-semibold whitespace-nowrap",
                isDestructiveAction(action)
                    ? "bg-destructive/12 text-destructive"
                    : "bg-surface-2 text-foreground",
            )}
        >
            {AUDIT_ACTION_LABELS[action]}
        </span>
    );
}
