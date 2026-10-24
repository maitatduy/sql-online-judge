import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { ApiFieldError } from "@/lib/api-client";

import { banAdminUser, isBanDuration } from "./admin-user-actions";
import {
    cannotModifyAdmin,
    cannotModifySelf,
    isOtherAdmin,
    reasonFieldError,
    userNotFound,
    validationFailed,
} from "./admin-user-errors";
import { changeAdminUser, findAdminUser, readAdminUserDetail } from "./admin-user-store";
import { apiError } from "./api-error";
import { readMockSession } from "./session";

export interface BanBody {
    duration?: unknown;
    reason?: unknown;
    notifyByEmail?: unknown;
}

export function banFieldError({ duration, reason, notifyByEmail }: BanBody): ApiFieldError | null {
    if (!isBanDuration(duration)) {
        return { field: "duration", message: "Thời hạn không hợp lệ" };
    }
    if (typeof notifyByEmail !== "boolean") {
        return { field: "notifyByEmail", message: "Phải là true hoặc false" };
    }
    return reasonFieldError(reason);
}

export const adminUserBanHandlers: RequestHandler[] = [
    http.post("*/api/admin/users/:username/ban", async ({ params, request }) => {
        await delay();
        const username = String(params.username);
        const body = (await request.json()) as BanBody;
        const invalid = banFieldError(body);
        if (invalid) {
            return validationFailed(invalid);
        }
        const user = findAdminUser(username);
        if (!user) {
            return userNotFound();
        }
        if (readMockSession()?.username === username) {
            return cannotModifySelf("Bạn không thể tự khoá tài khoản của chính mình");
        }
        if (isOtherAdmin(user)) {
            return cannotModifyAdmin("Không thể khoá tài khoản quản trị viên khác");
        }
        if (user.status === "BANNED") {
            return apiError(409, "USER_ALREADY_BANNED", "Tài khoản đã bị khoá");
        }
        if (isBanDuration(body.duration)) {
            banAdminUser(username, body.duration, String(body.reason));
        }
        return HttpResponse.json(readAdminUserDetail(username));
    }),

    http.post("*/api/admin/users/:username/unban", async ({ params, request }) => {
        await delay();
        const username = String(params.username);
        const { reason } = (await request.json()) as { reason?: unknown };
        const invalid = reasonFieldError(reason);
        if (invalid) {
            return validationFailed(invalid);
        }
        const user = findAdminUser(username);
        if (!user) {
            return userNotFound();
        }
        if (user.status !== "BANNED") {
            return apiError(409, "USER_NOT_BANNED", "Tài khoản không bị khoá");
        }
        changeAdminUser(username, { status: "ACTIVE", bannedUntil: null });
        return HttpResponse.json(readAdminUserDetail(username));
    }),
];
