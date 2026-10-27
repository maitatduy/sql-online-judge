export type InvitedRole = "SETTER" | "MODERATOR";

export type InvitationStatus = "PENDING" | "ACCEPTED" | "EXPIRED" | "REVOKED";

export interface RoleInvitationView {
    maskedEmail: string;
    role: InvitedRole;
    inviterUsername: string | null;
    expiresAt: string;
    status: InvitationStatus;
}

export interface AcceptInvitationResponse {
    role: InvitedRole;
}
