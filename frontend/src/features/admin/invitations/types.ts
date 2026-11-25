import type { Page } from "@/lib/pagination";

export type InvitableRole = "SETTER" | "MODERATOR";

export type InvitationStatus = "PENDING" | "ACCEPTED" | "DECLINED" | "EXPIRED" | "REVOKED";

export interface InviteRolesRequest {
    emails: string[];
    role: InvitableRole;
    message?: string;
}

export interface SkippedInvitation {
    email: string;
    reason: string;
}

export interface InviteRolesResponse {
    sentCount: number;
    skipped: SkippedInvitation[];
}

export interface RoleInvitationSummary {
    id: string;
    email: string;
    role: InvitableRole;
    status: InvitationStatus;
    inviterUsername: string | null;
    invitedAt: string;
    expiresAt: string;
    acceptedAt: string | null;
    revokedAt: string | null;
    declinedAt: string | null;
}

export type RoleInvitationsResponse = Page<RoleInvitationSummary>;

export interface RoleInvitationFilters {
    status: InvitationStatus | null;
    q: string;
    page: number;
}
