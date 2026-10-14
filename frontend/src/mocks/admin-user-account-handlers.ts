import { delay, http, HttpResponse, type RequestHandler } from "msw";

import { ROLES } from "@/features/admin/users/role-labels";
import type { AdminUserSummary } from "@/features/admin/users/types";
import type { ApiFieldError } from "@/lib/api-client";
import { USERNAME_PATTERN } from "@/lib/username";

import {
    cannotModifySelf,
    reasonFieldError,
    userNotFound,
    validationFailed,
} from "./admin-user-errors";
import {
    changeAdminUser,
    changeAdminUserProfile,
    createAdminUser,
    deleteAdminUser,
    findAdminUser,
    findAnyAdminUser,
    isTakenByAnyUser,
    readAdminUserDetail,
    restoreAdminUser,
} from "./admin-user-store";
import { apiError } from "./api-error";
import { readMockSession } from "./session";

const NO_CONTENT_STATUS = 204;
const CREATED_STATUS = 201;
const MAX_DISPLAY_NAME_LENGTH = 60;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface UpdateBody {
    role?: unknown;
    displayName?: unknown;
}

function isValidDisplayName(displayName: unknown): displayName is string | null {
    if (displayName === null) {
        return true;
    }
    if (typeof displayName !== "string") {
        return false;
    }
    const trimmedName = displayName.trim();
    return trimmedName !== "" && trimmedName.length <= MAX_DISPLAY_NAME_LENGTH;
}

function updateFieldError(body: UpdateBody): ApiFieldError | null {
    const hasRole = "role" in body;
    const hasDisplayName = "displayName" in body;
    if (!hasRole && !hasDisplayName) {
        return { field: "role", message: "Cần vai trò hoặc tên hiển thị" };
    }
    if (hasRole && !ROLES.some((role) => role === body.role)) {
        return { field: "role", message: "Vai trò không hợp lệ" };
    }
    if (hasDisplayName && !isValidDisplayName(body.displayName)) {
        return {
            field: "displayName",
            message: `Tên hiển thị từ 1 đến ${MAX_DISPLAY_NAME_LENGTH} ký tự`,
        };
    }
    return null;
}

interface CreateBody {
    username?: unknown;
    email?: unknown;
    role?: unknown;
    displayName?: unknown;
}

function createFieldError({
    username,
    email,
    role,
    displayName,
}: CreateBody): ApiFieldError | null {
    if (typeof username !== "string" || !USERNAME_PATTERN.test(username)) {
        return { field: "username", message: "Tên người dùng không hợp lệ" };
    }
    if (typeof email !== "string" || !EMAIL_PATTERN.test(email)) {
        return { field: "email", message: "Email không hợp lệ" };
    }
    if (!ROLES.some((knownRole) => knownRole === role)) {
        return { field: "role", message: "Vai trò không hợp lệ" };
    }
    if (displayName !== undefined && (displayName === null || !isValidDisplayName(displayName))) {
        return {
            field: "displayName",
            message: `Tên hiển thị từ 1 đến ${MAX_DISPLAY_NAME_LENGTH} ký tự`,
        };
    }
    return null;
}

function newUser(
    username: string,
    email: string,
    role: AdminUserSummary["role"],
): AdminUserSummary {
    const now = new Date().toISOString();
    return {
        id: `0192f3a2-0000-7000-8000-${Date.now().toString(16).padStart(12, "0")}`,
        username,
        email,
        role,
        rating: 0,
        acceptedCount: 0,
        joinedAt: now,
        lastActiveAt: now,
        isOnline: false,
        status: "ACTIVE",
        bannedUntil: null,
        isEmailVerified: false,
        avatarUrl: null,
        deletedAt: null,
    };
}

export const adminUserAccountHandlers: RequestHandler[] = [
    http.post("*/api/admin/users", async ({ request }) => {
        await delay();
        const body = (await request.json()) as CreateBody;
        const invalid = createFieldError(body);
        const role = ROLES.find((knownRole) => knownRole === body.role);
        if (invalid || !role) {
            return validationFailed(invalid ?? { field: "role", message: "Vai trò không hợp lệ" });
        }
        const username = String(body.username);
        const email = String(body.email);
        if (isTakenByAnyUser((user) => user.username === username)) {
            return apiError(409, "USERNAME_TAKEN", "Tên người dùng đã có người dùng");
        }
        if (isTakenByAnyUser((user) => user.email.toLowerCase() === email.toLowerCase())) {
            return apiError(409, "EMAIL_TAKEN", "Email đã được dùng cho tài khoản khác");
        }
        const displayName = typeof body.displayName === "string" ? body.displayName.trim() : null;
        createAdminUser(newUser(username, email, role), { displayName });
        return HttpResponse.json(readAdminUserDetail(username), { status: CREATED_STATUS });
    }),

    http.patch("*/api/admin/users/:username", async ({ params, request }) => {
        await delay();
        const username = String(params.username);
        const body = (await request.json()) as UpdateBody;
        const invalid = updateFieldError(body);
        if (invalid) {
            return validationFailed(invalid);
        }
        const user = findAdminUser(username);
        if (!user) {
            return userNotFound();
        }
        const newRole = ROLES.find((role) => role === body.role);
        if (newRole && newRole !== user.role && readMockSession()?.username === username) {
            return cannotModifySelf("Bạn không thể tự đổi vai trò của chính mình");
        }
        if (newRole) {
            changeAdminUser(username, { role: newRole });
        }
        if ("displayName" in body && isValidDisplayName(body.displayName)) {
            changeAdminUserProfile(username, { displayName: body.displayName?.trim() ?? null });
        }
        return HttpResponse.json(readAdminUserDetail(username));
    }),

    http.delete("*/api/admin/users/:username", async ({ params, request }) => {
        await delay();
        const username = String(params.username);
        const { reason } = (await request.json()) as { reason?: unknown };
        const invalid = reasonFieldError(reason);
        if (invalid) {
            return validationFailed(invalid);
        }
        if (!findAdminUser(username)) {
            return userNotFound();
        }
        if (readMockSession()?.username === username) {
            return cannotModifySelf("Bạn không thể tự xoá tài khoản của chính mình");
        }
        deleteAdminUser(username);
        return new HttpResponse(null, { status: NO_CONTENT_STATUS });
    }),

    http.post("*/api/admin/users/:username/restore", async ({ params }) => {
        await delay();
        const username = String(params.username);
        const user = findAnyAdminUser(username);
        if (!user) {
            return userNotFound();
        }
        if (!user.deletedAt) {
            return apiError(409, "USER_NOT_DELETED", "Tài khoản chưa bị xoá");
        }
        restoreAdminUser(username);
        return HttpResponse.json(readAdminUserDetail(username));
    }),
];
