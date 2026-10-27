import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    InvitableRole,
    InvitationStatus,
    InviteRolesRequest,
    InviteRolesResponse,
    RoleInvitationsResponse,
    RoleInvitationSummary,
} from "@/features/admin/invitations/types";

import { apiError } from "./api-error";
import { readMockSession } from "./session";

const STORAGE_KEY = "sqljudge:mock:role-invitations";
const DAY_MS = 24 * 60 * 60_000;
const TTL_MS = 7 * DAY_MS;
const DEFAULT_PAGE_SIZE = 20;

interface StoredInvitation {
    id: string;
    email: string;
    role: InvitableRole;
    inviterUsername: string;
    invitedAtMs: number;
    expiresAtMs: number;
    acceptedAtMs: number | null;
    revokedAtMs: number | null;
}

function seed(
    id: string,
    email: string,
    role: InvitableRole,
    invitedDaysAgo: number,
    extra: Partial<StoredInvitation> = {},
): StoredInvitation {
    const invitedAtMs = Date.now() - invitedDaysAgo * DAY_MS;
    return {
        id,
        email,
        role,
        inviterUsername: "admin.huy",
        invitedAtMs,
        expiresAtMs: invitedAtMs + TTL_MS,
        acceptedAtMs: null,
        revokedAtMs: null,
        ...extra,
    };
}

function seededInvitations(): StoredInvitation[] {
    return [
        seed("inv-giang", "giang.vo@gmail.com", "SETTER", 1),
        seed("inv-linh", "linh.tran@gmail.com", "MODERATOR", 2),
        seed("inv-an", "an.nguyen@gmail.com", "SETTER", 3, { acceptedAtMs: Date.now() - DAY_MS }),
        seed("inv-binh", "binh.le@gmail.com", "SETTER", 9),
        seed("inv-chi", "chi.pham@gmail.com", "MODERATOR", 4, { revokedAtMs: Date.now() - DAY_MS }),
    ];
}

function readInvitations(): StoredInvitation[] {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as StoredInvitation[]) : seededInvitations();
}

function writeInvitations(invitations: StoredInvitation[]): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(invitations));
}

function statusOf(invitation: StoredInvitation, nowMs: number): InvitationStatus {
    if (invitation.acceptedAtMs !== null) {
        return "ACCEPTED";
    }
    if (invitation.revokedAtMs !== null) {
        return "REVOKED";
    }
    return nowMs < invitation.expiresAtMs ? "PENDING" : "EXPIRED";
}

function toSummary(invitation: StoredInvitation, nowMs: number): RoleInvitationSummary {
    const iso = (ms: number | null) => (ms === null ? null : new Date(ms).toISOString());
    return {
        id: invitation.id,
        email: invitation.email,
        role: invitation.role,
        status: statusOf(invitation, nowMs),
        inviterUsername: invitation.inviterUsername,
        invitedAt: new Date(invitation.invitedAtMs).toISOString(),
        expiresAt: new Date(invitation.expiresAtMs).toISOString(),
        acceptedAt: iso(invitation.acceptedAtMs),
        revokedAt: iso(invitation.revokedAtMs),
    };
}

function updateInvitation(
    id: string,
    change: (invitation: StoredInvitation, nowMs: number) => StoredInvitation | string,
) {
    const nowMs = Date.now();
    const invitations = readInvitations();
    const index = invitations.findIndex((invitation) => invitation.id === id);
    if (index < 0) {
        return apiError(404, "INVITATION_NOT_FOUND", "Không tìm thấy lời mời");
    }
    const changed = change(invitations[index], nowMs);
    if (typeof changed === "string") {
        return apiError(409, "INVITATION_NOT_PENDING", changed);
    }
    invitations[index] = changed;
    writeInvitations(invitations);
    return HttpResponse.json(toSummary(changed, nowMs));
}

export const adminRoleInvitationHandlers: RequestHandler[] = [
    http.get("*/api/admin/role-invitations", async ({ request }) => {
        await delay();
        const query = new URL(request.url).searchParams;
        const status = query.get("status");
        const keyword = query.get("q")?.trim().toLowerCase() ?? "";
        const page = Number(query.get("page") ?? 1);
        const size = Number(query.get("size") ?? DEFAULT_PAGE_SIZE);
        const nowMs = Date.now();
        const matching = readInvitations()
            .map((invitation) => toSummary(invitation, nowMs))
            .filter((invitation) => status === null || invitation.status === status)
            .filter((invitation) => invitation.email.includes(keyword))
            .sort((first, second) => second.invitedAt.localeCompare(first.invitedAt));
        const response: RoleInvitationsResponse = {
            items: matching.slice((page - 1) * size, page * size),
            page,
            size,
            totalItems: matching.length,
            totalPages: Math.ceil(matching.length / size),
        };
        return HttpResponse.json(response);
    }),

    http.post("*/api/admin/role-invitations", async ({ request }) => {
        await delay();
        const body = (await request.json()) as InviteRolesRequest;
        const nowMs = Date.now();
        const emails = [...new Set(body.emails.map((email) => email.toLowerCase()))];
        const added = emails.map((email) => ({
            ...seed(`inv-${nowMs}-${email}`, email, body.role, 0),
            inviterUsername: readMockSession()?.username ?? "admin.huy",
        }));
        writeInvitations([...added, ...readInvitations()]);
        const response: InviteRolesResponse = { sentCount: added.length, skipped: [] };
        return HttpResponse.json(response);
    }),

    http.post("*/api/admin/role-invitations/:id/revoke", async ({ params }) => {
        await delay();
        const response = updateInvitation(String(params.id), (invitation, nowMs) =>
            statusOf(invitation, nowMs) === "PENDING"
                ? { ...invitation, revokedAtMs: nowMs }
                : "Chỉ huỷ được lời mời đang chờ",
        );
        return response.status === 200 ? new HttpResponse(null, { status: 204 }) : response;
    }),

    http.post("*/api/admin/role-invitations/:id/resend", async ({ params }) => {
        await delay();
        return updateInvitation(String(params.id), (invitation, nowMs) =>
            ["PENDING", "EXPIRED"].includes(statusOf(invitation, nowMs))
                ? { ...invitation, expiresAtMs: nowMs + TTL_MS }
                : "Chỉ gửi lại được lời mời đang chờ hoặc đã hết hạn",
        );
    }),
];
