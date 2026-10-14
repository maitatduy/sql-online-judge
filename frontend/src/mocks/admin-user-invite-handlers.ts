import { delay, http, HttpResponse, type RequestHandler } from "msw";

import { invalidEmailsOf } from "@/features/admin/users/email-list";
import type { InviteUsersResponse } from "@/features/admin/users/types";
import type { ApiFieldError } from "@/lib/api-client";

import { validationFailed } from "./admin-user-errors";

const MAX_INVITE_EMAILS = 50;
const MAX_MESSAGE_LENGTH = 300;
const INVITE_ROLES = ["SETTER", "MODERATOR"];
const CREATED_STATUS = 201;

interface InviteBody {
    emails?: unknown;
    role?: unknown;
    message?: unknown;
}

function isEmailList(emails: unknown): emails is string[] {
    return Array.isArray(emails) && emails.every((email) => typeof email === "string");
}

function inviteFieldError({ emails, role, message }: InviteBody): ApiFieldError | null {
    if (!isEmailList(emails) || emails.length === 0 || emails.length > MAX_INVITE_EMAILS) {
        return { field: "emails", message: `Nhập từ 1 đến ${MAX_INVITE_EMAILS} email` };
    }
    const invalidEmails = invalidEmailsOf(emails);
    if (invalidEmails.length > 0) {
        return { field: "emails", message: `Email không hợp lệ: ${invalidEmails.join(", ")}` };
    }
    if (typeof role !== "string" || !INVITE_ROLES.includes(role)) {
        return { field: "role", message: "Chỉ mời được người ra đề hoặc kiểm duyệt" };
    }
    if (
        message !== undefined &&
        (typeof message !== "string" || message.length > MAX_MESSAGE_LENGTH)
    ) {
        return { field: "message", message: `Lời nhắn tối đa ${MAX_MESSAGE_LENGTH} ký tự` };
    }
    return null;
}

export const adminUserInviteHandlers: RequestHandler[] = [
    http.post("*/api/admin/user-invitations", async ({ request }) => {
        await delay();
        const body = (await request.json()) as InviteBody;
        const invalid = inviteFieldError(body);
        if (invalid || !isEmailList(body.emails)) {
            return validationFailed(invalid ?? { field: "emails", message: "Không hợp lệ" });
        }
        const uniqueEmails = new Set(body.emails.map((email) => email.toLowerCase()));
        const response: InviteUsersResponse = { invitedCount: uniqueEmails.size };
        return HttpResponse.json(response, { status: CREATED_STATUS });
    }),
];
