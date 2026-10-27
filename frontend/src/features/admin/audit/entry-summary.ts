import type { AuditAction, AuditLogEntry } from "./types";

const CHANGED_FIELDS: Partial<Record<AuditAction, string>> = {
    UPDATE_ROLE: "role",
    UPDATE_DISPLAY_NAME: "displayName",
    UPDATE_RESTRICTIONS: "restrictions",
    INVITE_ROLE: "role",
    ACCEPT_ROLE_INVITATION: "role",
    REVOKE_ROLE_INVITATION: "role",
    RESEND_ROLE_INVITATION: "role",
};

export function hasValueChange({ previousValue, newValue }: AuditLogEntry): boolean {
    return previousValue !== null || newValue !== null;
}

export function changedFieldOf(action: AuditAction): string {
    return CHANGED_FIELDS[action] ?? "value";
}

export function targetSummaryOf(entry: AuditLogEntry): string {
    const target = entry.targetUsername ?? entry.details ?? "-";
    if (!hasValueChange(entry)) {
        return target;
    }
    return `${target}: ${entry.previousValue ?? "-"} → ${entry.newValue ?? "-"}`;
}
