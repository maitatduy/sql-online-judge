import { delay, http, HttpResponse, type RequestHandler } from "msw";

import { ROLES } from "@/features/admin/users/role-labels";
import { parseUsersSort, type UsersSort } from "@/features/admin/users/sort";
import { accountStateOf } from "@/features/admin/users/account-status";
import type {
    AdminUsersResponse,
    AdminUserSummary,
    UserCountsResponse,
} from "@/features/admin/users/types";
import type { ApiFieldError } from "@/lib/api-client";

import { adminUserBanHandlers } from "./admin-user-ban-handlers";
import { adminUserEmailHandlers } from "./admin-user-email-handlers";
import { cannotModifySelf, userNotFound, validationFailed } from "./admin-user-errors";
import {
    changeAdminUser,
    findAdminUser,
    readAdminUserDetail,
    readAdminUsers,
} from "./admin-user-store";
import { readMockSession } from "./session";

const DEFAULT_PAGE_SIZE = 20;
const MAX_PAGE_SIZE = 100;
const ACCOUNT_STATUSES = ["ACTIVE", "BANNED"];
const BOOLEAN_VALUES = ["true", "false"];

function integerParam(value: string | null, fallback: number): number {
    return value === null ? fallback : Number(value);
}

function isUnknown(knownValues: string[], value: string | null): boolean {
    return value !== null && !knownValues.includes(value);
}

function invalidParam(query: URLSearchParams): ApiFieldError | null {
    const page = integerParam(query.get("page"), 1);
    const size = integerParam(query.get("size"), DEFAULT_PAGE_SIZE);
    if (!Number.isInteger(page) || page < 1) {
        return { field: "page", message: "Phải từ 1 trở lên" };
    }
    if (!Number.isInteger(size) || size < 1 || size > MAX_PAGE_SIZE) {
        return { field: "size", message: `Phải từ 1 đến ${MAX_PAGE_SIZE}` };
    }
    if (isUnknown(ROLES, query.get("role"))) {
        return { field: "role", message: "Vai trò không hợp lệ" };
    }
    if (isUnknown(ACCOUNT_STATUSES, query.get("status"))) {
        return { field: "status", message: "Trạng thái không hợp lệ" };
    }
    if (isUnknown(BOOLEAN_VALUES, query.get("emailVerified"))) {
        return { field: "emailVerified", message: "Phải là true hoặc false" };
    }
    if (query.has("sort") && !parseUsersSort(query.get("sort"))) {
        return { field: "sort", message: "Cách sắp xếp không hợp lệ" };
    }
    return null;
}

function compareUsers(sort: UsersSort | null) {
    return (first: AdminUserSummary, second: AdminUserSummary): number => {
        const newestFirst = second.joinedAt.localeCompare(first.joinedAt);
        if (!sort) {
            return newestFirst;
        }
        const ascending = first[sort.field] - second[sort.field];
        return (sort.direction === "asc" ? ascending : -ascending) || newestFirst;
    };
}

function matchesQuery(user: AdminUserSummary, query: URLSearchParams): boolean {
    const keyword = query.get("q")?.trim().toLowerCase() ?? "";
    const role = query.get("role");
    const status = query.get("status");
    const emailVerified = query.get("emailVerified");
    return (
        (user.username.includes(keyword) || user.email.includes(keyword)) &&
        (role === null || user.role === role) &&
        (status === null || user.status === status) &&
        (emailVerified === null || String(user.isEmailVerified) === emailVerified)
    );
}

function countUsers(users: AdminUserSummary[]): UserCountsResponse {
    const countWhere = (isCounted: (user: AdminUserSummary) => boolean) =>
        users.filter(isCounted).length;
    return {
        total: users.length,
        unverified: countWhere((user) => accountStateOf(user) === "UNVERIFIED"),
        setters: countWhere((user) => user.role === "SETTER"),
        moderators: countWhere((user) => user.role === "MODERATOR"),
        banned: countWhere((user) => user.status === "BANNED"),
    };
}

export const adminUserHandlers: RequestHandler[] = [
    http.get("*/api/admin/users/summary", async () => {
        await delay();
        return HttpResponse.json(countUsers(readAdminUsers(new Date())));
    }),

    http.get("*/api/admin/users/:username", async ({ params }) => {
        await delay();
        const user = readAdminUserDetail(String(params.username));
        return user ? HttpResponse.json(user) : userNotFound();
    }),

    http.patch("*/api/admin/users/:username", async ({ params, request }) => {
        await delay();
        const username = String(params.username);
        const { role } = (await request.json()) as { role?: unknown };
        const newRole = ROLES.find((knownRole) => knownRole === role);
        if (!newRole) {
            return validationFailed({ field: "role", message: "Vai trò không hợp lệ" });
        }
        if (!findAdminUser(username)) {
            return userNotFound();
        }
        if (readMockSession()?.username === username) {
            return cannotModifySelf("Bạn không thể tự đổi vai trò của chính mình");
        }
        changeAdminUser(username, { role: newRole });
        return HttpResponse.json(readAdminUserDetail(username));
    }),

    ...adminUserBanHandlers,
    ...adminUserEmailHandlers,

    http.get("*/api/admin/users", async ({ request }) => {
        await delay();
        const query = new URL(request.url).searchParams;
        const invalid = invalidParam(query);
        if (invalid) {
            return validationFailed(invalid);
        }
        const page = integerParam(query.get("page"), 1);
        const size = integerParam(query.get("size"), DEFAULT_PAGE_SIZE);
        const users = readAdminUsers(new Date())
            .filter((user) => matchesQuery(user, query))
            .sort(compareUsers(parseUsersSort(query.get("sort"))));
        const firstIndex = (page - 1) * size;
        const response: AdminUsersResponse = {
            items: users.slice(firstIndex, firstIndex + size),
            page,
            size,
            totalItems: users.length,
            totalPages: Math.ceil(users.length / size),
        };
        return HttpResponse.json(response);
    }),
];
