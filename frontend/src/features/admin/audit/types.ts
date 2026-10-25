import type { Role } from "@/lib/current-user";
import type { Page } from "@/lib/pagination";

export type AuditCategory = "ACCOUNT";

export interface AuditActorsResponse {
    usernames: string[];
}

export type AuditAction =
    | "BAN"
    | "UNBAN"
    | "REVOKE_SESSION"
    | "REVOKE_ALL_SESSIONS"
    | "UPDATE_ROLE"
    | "UPDATE_DISPLAY_NAME"
    | "SEND_PASSWORD_RESET"
    | "SEND_VERIFICATION"
    | "DELETE"
    | "RESTORE"
    | "UPDATE_AVATAR"
    | "REMOVE_AVATAR"
    | "EXPORT_USER_EMAILS"
    | "UPDATE_RESTRICTIONS"
    | "EXPORT_AUDIT_LOG";

export interface AuditLogEntry {
    id: string;
    occurredAt: string;
    actorUsername: string;
    actorRole: Role;
    targetUsername: string | null;
    action: AuditAction;
    reason: string | null;
    previousValue: string | null;
    newValue: string | null;
    expiresAt: string | null;
    details: string | null;
}

export type AuditLogResponse = Page<AuditLogEntry>;
