import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { UserRestrictions } from "@/features/admin/users/types";
import type { ApiFieldError } from "@/lib/api-client";

import {
    cannotModifyAdmin,
    cannotModifySelf,
    isOtherAdmin,
    reasonFieldError,
    userNotFound,
    validationFailed,
} from "./admin-user-errors";
import { findAdminUser, findAnyAdminUser } from "./admin-user-store";
import { readMockSession } from "./session";

const RESTRICTIONS_STORAGE_KEY = "sqljudge:mock:admin-user-restrictions";
const DEFAULT_RESTRICTIONS: UserRestrictions = {
    aiDailyQuota: 20,
    isBannedFromRatedContests: false,
};
const MAX_AI_DAILY_QUOTA = 200;

interface RestrictionsBody {
    aiDailyQuota?: unknown;
    isBannedFromRatedContests?: unknown;
    reason?: unknown;
}

function readAllRestrictions(): Record<string, UserRestrictions> {
    const stored = sessionStorage.getItem(RESTRICTIONS_STORAGE_KEY);
    return stored ? (JSON.parse(stored) as Record<string, UserRestrictions>) : {};
}

function restrictionsOf(username: string): UserRestrictions {
    return { ...DEFAULT_RESTRICTIONS, ...readAllRestrictions()[username] };
}

function isValidQuota(quota: unknown): quota is number {
    return Number.isInteger(quota) && Number(quota) >= 0 && Number(quota) <= MAX_AI_DAILY_QUOTA;
}

function restrictionsFieldError(body: RestrictionsBody): ApiFieldError | null {
    const hasQuota = "aiDailyQuota" in body;
    const hasRatedBan = "isBannedFromRatedContests" in body;
    if (!hasQuota && !hasRatedBan) {
        return { field: "aiDailyQuota", message: "Cần ít nhất một thay đổi" };
    }
    if (hasQuota && !isValidQuota(body.aiDailyQuota)) {
        return { field: "aiDailyQuota", message: `Hạn mức AI từ 0 đến ${MAX_AI_DAILY_QUOTA}` };
    }
    if (hasRatedBan && typeof body.isBannedFromRatedContests !== "boolean") {
        return { field: "isBannedFromRatedContests", message: "Phải là true hoặc false" };
    }
    return reasonFieldError(body.reason);
}

export const adminUserRestrictionHandlers: RequestHandler[] = [
    http.get("*/api/admin/users/:username/restrictions", async ({ params }) => {
        await delay();
        const user = findAnyAdminUser(String(params.username));
        return user ? HttpResponse.json(restrictionsOf(user.username)) : userNotFound();
    }),

    http.patch("*/api/admin/users/:username/restrictions", async ({ params, request }) => {
        await delay();
        const username = String(params.username);
        const body = (await request.json()) as RestrictionsBody;
        const invalid = restrictionsFieldError(body);
        if (invalid) {
            return validationFailed(invalid);
        }
        const user = findAdminUser(username);
        if (!user) {
            return userNotFound();
        }
        if (readMockSession()?.username === username) {
            return cannotModifySelf("Bạn không thể tự đặt hạn chế cho chính mình");
        }
        if (isOtherAdmin(user)) {
            return cannotModifyAdmin("Không thể đặt hạn chế cho quản trị viên khác");
        }
        const updated: UserRestrictions = {
            ...restrictionsOf(username),
            ...(isValidQuota(body.aiDailyQuota) ? { aiDailyQuota: body.aiDailyQuota } : {}),
            ...(typeof body.isBannedFromRatedContests === "boolean"
                ? { isBannedFromRatedContests: body.isBannedFromRatedContests }
                : {}),
        };
        sessionStorage.setItem(
            RESTRICTIONS_STORAGE_KEY,
            JSON.stringify({ ...readAllRestrictions(), [username]: updated }),
        );
        return HttpResponse.json(updated);
    }),
];
