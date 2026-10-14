import { delay, http, HttpResponse, type RequestHandler } from "msw";

import { ROLES } from "@/features/admin/users/role-labels";
import type { AdminUserSummary, BulkUserActionResponse } from "@/features/admin/users/types";

import { banAdminUser, isBanDuration, usernamesFieldError } from "./admin-user-actions";
import { banFieldError, type BanBody } from "./admin-user-ban-handlers";
import { reasonFieldError, validationFailed } from "./admin-user-errors";
import { changeAdminUser, findAdminUser } from "./admin-user-store";
import { readMockSession } from "./session";

function applyToUsers(
    usernames: string[],
    canApply: (user: AdminUserSummary) => boolean,
    apply: (username: string) => void,
): BulkUserActionResponse {
    const selfUsername = readMockSession()?.username;
    const skippedUsernames: string[] = [];
    usernames.forEach((username) => {
        const user = findAdminUser(username);
        if (!user || username === selfUsername || !canApply(user)) {
            skippedUsernames.push(username);
            return;
        }
        apply(username);
    });
    return { affectedCount: usernames.length - skippedUsernames.length, skippedUsernames };
}

export const adminUserBulkHandlers: RequestHandler[] = [
    http.post("*/api/admin/users/bulk-ban", async ({ request }) => {
        await delay();
        const body = (await request.json()) as BanBody & { usernames?: unknown };
        const invalid = usernamesFieldError(body.usernames) ?? banFieldError(body);
        if (invalid || !isBanDuration(body.duration)) {
            return validationFailed(invalid ?? { field: "duration", message: "Không hợp lệ" });
        }
        const duration = body.duration;
        return HttpResponse.json(
            applyToUsers(
                body.usernames as string[],
                (user) => user.status !== "BANNED",
                (username) => banAdminUser(username, duration, String(body.reason)),
            ),
        );
    }),

    http.post("*/api/admin/users/bulk-role", async ({ request }) => {
        await delay();
        const body = (await request.json()) as {
            usernames?: unknown;
            role?: unknown;
            reason?: unknown;
        };
        const role = ROLES.find((knownRole) => knownRole === body.role);
        const invalid =
            usernamesFieldError(body.usernames) ??
            (role ? null : { field: "role", message: "Vai trò không hợp lệ" }) ??
            reasonFieldError(body.reason);
        if (invalid || !role) {
            return validationFailed(invalid ?? { field: "role", message: "Vai trò không hợp lệ" });
        }
        return HttpResponse.json(
            applyToUsers(
                body.usernames as string[],
                () => true,
                (username) => changeAdminUser(username, { role }),
            ),
        );
    }),
];
