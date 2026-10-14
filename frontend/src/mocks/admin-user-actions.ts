import type { BanDuration } from "@/features/admin/users/types";
import type { ApiFieldError } from "@/lib/api-client";
import { formatDate } from "@/lib/format";

import { addUserViolation, changeAdminUser } from "./admin-user-store";
import { readMockSession } from "./session";

const DAY_MS = 24 * 60 * 60_000;
const MAX_BULK_USERS = 100;
const BAN_DAYS: Record<BanDuration, number | null> = { DAYS_7: 7, DAYS_30: 30, PERMANENT: null };

export function isBanDuration(duration: unknown): duration is BanDuration {
    return typeof duration === "string" && duration in BAN_DAYS;
}

export function banAdminUser(username: string, duration: BanDuration, reason: string): void {
    const days = BAN_DAYS[duration];
    const bannedUntil = days === null ? null : new Date(Date.now() + days * DAY_MS).toISOString();
    changeAdminUser(username, { status: "BANNED", bannedUntil });
    addUserViolation(username, {
        severity: "SEVERE",
        title: bannedUntil ? `Bị khoá đến ${formatDate(bannedUntil)}` : "Bị khoá vĩnh viễn",
        detail: reason.trim(),
        status: "RESOLVED",
        handledBy: readMockSession()?.username ?? null,
        handledAt: new Date().toISOString(),
    });
}

export function usernamesFieldError(usernames: unknown): ApiFieldError | null {
    const isValidList =
        Array.isArray(usernames) &&
        usernames.length > 0 &&
        usernames.length <= MAX_BULK_USERS &&
        usernames.every((username) => typeof username === "string");
    return isValidList
        ? null
        : { field: "usernames", message: `Chọn từ 1 đến ${MAX_BULK_USERS} tài khoản` };
}
