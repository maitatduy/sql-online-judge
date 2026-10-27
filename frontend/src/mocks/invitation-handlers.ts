import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { InvitationStatus, RoleInvitationView } from "@/features/invitations/types";

import { apiError } from "./api-error";
import { readMockSession, writeMockSession } from "./session";

const INVITEE_USERNAME = "viet.luu";
const WEEK_MS = 7 * 24 * 60 * 60_000;

const STATUS_BY_TOKEN: Record<string, InvitationStatus> = {
    "loi-moi-dang-cho": "PENDING",
    "loi-moi-het-han": "EXPIRED",
    "loi-moi-da-huy": "REVOKED",
    "loi-moi-da-nhan": "ACCEPTED",
};

function invitationOf(token: string): RoleInvitationView | null {
    const status = STATUS_BY_TOKEN[token];
    if (!status) {
        return null;
    }
    return {
        maskedEmail: "vi***@example.com",
        role: "SETTER",
        inviterUsername: "admin.huy",
        expiresAt: new Date(Date.now() + WEEK_MS).toISOString(),
        status,
    };
}

export const invitationHandlers: RequestHandler[] = [
    http.get("*/api/role-invitations/:token", async ({ params }) => {
        await delay();
        const invitation = invitationOf(String(params.token));
        return invitation
            ? HttpResponse.json(invitation)
            : apiError(404, "INVITATION_NOT_FOUND", "Không tìm thấy lời mời");
    }),

    http.post("*/api/role-invitations/:token/accept", async ({ params }) => {
        await delay();
        const invitation = invitationOf(String(params.token));
        const currentUser = readMockSession();
        if (!invitation) {
            return apiError(404, "INVITATION_NOT_FOUND", "Không tìm thấy lời mời");
        }
        if (!currentUser) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        if (currentUser.username !== INVITEE_USERNAME) {
            return apiError(
                403,
                "INVITATION_EMAIL_MISMATCH",
                "Lời mời này gửi tới email khác với tài khoản đang đăng nhập",
            );
        }
        writeMockSession({ ...currentUser, role: invitation.role });
        return HttpResponse.json({ role: invitation.role });
    }),
];
