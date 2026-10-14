import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { BanDuration } from "@/features/admin/users/types";
import type { ApiFieldError } from "@/lib/api-client";
import { formatDate } from "@/lib/format";

import {
    cannotModifySelf,
    reasonFieldError,
    userNotFound,
    validationFailed,
} from "./admin-user-errors";
import {
    addUserViolation,
    changeAdminUser,
    findAdminUser,
    readAdminUserDetail,
} from "./admin-user-store";
import { apiError } from "./api-error";
import { readMockSession } from "./session";

const DAY_MS = 24 * 60 * 60_000;
const BAN_DAYS: Record<BanDuration, number | null> = { DAYS_7: 7, DAYS_30: 30, PERMANENT: null };

interface BanBody {
    duration?: unknown;
    reason?: unknown;
    notifyByEmail?: unknown;
}

function isBanDuration(duration: unknown): duration is BanDuration {
    return typeof duration === "string" && duration in BAN_DAYS;
}

function banFieldError({ duration, reason, notifyByEmail }: BanBody): ApiFieldError | null {
    if (!isBanDuration(duration)) {
        return { field: "duration", message: "Thời hạn không hợp lệ" };
    }
    if (typeof notifyByEmail !== "boolean") {
        return { field: "notifyByEmail", message: "Phải là true hoặc false" };
    }
    return reasonFieldError(reason);
}

function bannedUntilFor(duration: BanDuration, now: Date): string | null {
    const days = BAN_DAYS[duration];
    return days === null ? null : new Date(now.getTime() + days * DAY_MS).toISOString();
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
        if (user.status === "BANNED") {
            return apiError(409, "USER_ALREADY_BANNED", "Tài khoản đã bị khoá");
        }
        const bannedUntil = bannedUntilFor(body.duration as BanDuration, new Date());
        changeAdminUser(username, { status: "BANNED", bannedUntil });
        addUserViolation(username, {
            severity: "SEVERE",
            title: bannedUntil ? `Bị khoá đến ${formatDate(bannedUntil)}` : "Bị khoá vĩnh viễn",
            detail: String(body.reason).trim(),
        });
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
