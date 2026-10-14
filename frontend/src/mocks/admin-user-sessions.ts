import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AdminUserSummary, UserSession } from "@/features/admin/users/types";

import { userNotFound } from "./admin-user-errors";
import { findAdminUser, findAnyAdminUser } from "./admin-user-store";
import { apiError } from "./api-error";

const REVOKED_STORAGE_KEY = "sqljudge:mock:admin-user-revoked-sessions";
const DAY_MS = 24 * 60 * 60_000;
const NO_CONTENT_STATUS = 204;

function readRevoked(): Record<string, string[]> {
    const stored = sessionStorage.getItem(REVOKED_STORAGE_KEY);
    return stored ? (JSON.parse(stored) as Record<string, string[]>) : {};
}

function revokeSessions(username: string, sessionIds: string[]): void {
    const revoked = readRevoked();
    sessionStorage.setItem(
        REVOKED_STORAGE_KEY,
        JSON.stringify({ ...revoked, [username]: [...(revoked[username] ?? []), ...sessionIds] }),
    );
}

function seededSessions(user: AdminUserSummary, now: Date): UserSession[] {
    const daysAgo = (days: number) => new Date(now.getTime() - days * DAY_MS).toISOString();
    if (user.username === "viet.luu") {
        return [
            {
                id: "viet.luu-s1",
                browser: "Chrome",
                os: "Windows",
                city: "Hà Nội",
                lastActiveAt: user.lastActiveAt,
                isActiveNow: true,
                isUnusualLocation: false,
            },
            {
                id: "viet.luu-s2",
                browser: "Safari",
                os: "iPhone",
                city: "Hà Nội",
                lastActiveAt: daysAgo(1),
                isActiveNow: false,
                isUnusualLocation: false,
            },
            {
                id: "viet.luu-s3",
                browser: "Chrome",
                os: "Windows",
                city: "TP.HCM",
                lastActiveAt: daysAgo(4),
                isActiveNow: false,
                isUnusualLocation: true,
            },
        ];
    }
    return [
        {
            id: `${user.username}-s1`,
            browser: "Chrome",
            os: "Windows",
            city: "Hà Nội",
            lastActiveAt: user.lastActiveAt,
            isActiveNow: user.isOnline,
            isUnusualLocation: false,
        },
    ];
}

function activeSessionsOf(user: AdminUserSummary): UserSession[] {
    const revoked = readRevoked()[user.username] ?? [];
    return seededSessions(user, new Date()).filter((session) => !revoked.includes(session.id));
}

export const adminUserSessionHandlers: RequestHandler[] = [
    http.get("*/api/admin/users/:username/sessions", async ({ params }) => {
        await delay();
        const user = findAnyAdminUser(String(params.username));
        return user ? HttpResponse.json({ items: activeSessionsOf(user) }) : userNotFound();
    }),

    http.delete("*/api/admin/users/:username/sessions/:sessionId", async ({ params }) => {
        await delay();
        const user = findAdminUser(String(params.username));
        if (!user) {
            return userNotFound();
        }
        const sessionId = String(params.sessionId);
        if (!activeSessionsOf(user).some((session) => session.id === sessionId)) {
            return apiError(404, "SESSION_NOT_FOUND", "Không tìm thấy phiên đăng nhập");
        }
        revokeSessions(user.username, [sessionId]);
        return new HttpResponse(null, { status: NO_CONTENT_STATUS });
    }),

    http.delete("*/api/admin/users/:username/sessions", async ({ params }) => {
        await delay();
        const user = findAdminUser(String(params.username));
        if (!user) {
            return userNotFound();
        }
        revokeSessions(
            user.username,
            activeSessionsOf(user).map((session) => session.id),
        );
        return new HttpResponse(null, { status: NO_CONTENT_STATUS });
    }),
];
