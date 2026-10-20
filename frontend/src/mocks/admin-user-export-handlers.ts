import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AdminUserSummary } from "@/features/admin/users/types";
import type { ApiFieldError } from "@/lib/api-client";

import { reasonFieldError, validationFailed } from "./admin-user-errors";
import { queryAdminUsers } from "./admin-user-query";
import { readAdminUsers } from "./admin-user-store";
import { toCsv } from "./csv";

const EXPORT_SCOPES = ["FILTERED", "SELECTED", "ALL"];
const MAX_EXPORT_USERNAMES = 1000;
const BASE_COLUMNS = ["role", "status", "joinedAt"] as const;

interface ExportBody {
    scope?: unknown;
    filters?: unknown;
    usernames?: unknown;
    includeEmail?: unknown;
    reason?: unknown;
}

function isUsernameList(usernames: unknown): boolean {
    return (
        Array.isArray(usernames) &&
        usernames.length > 0 &&
        usernames.length <= MAX_EXPORT_USERNAMES &&
        usernames.every((username) => typeof username === "string")
    );
}

function exportFieldError({
    scope,
    filters,
    usernames,
    includeEmail,
    reason,
}: ExportBody): ApiFieldError | null {
    if (typeof scope !== "string" || !EXPORT_SCOPES.includes(scope)) {
        return { field: "scope", message: "Phạm vi xuất không hợp lệ" };
    }
    if (scope === "SELECTED" && !isUsernameList(usernames)) {
        return {
            field: "usernames",
            message: `Chọn từ 1 đến ${MAX_EXPORT_USERNAMES} tài khoản`,
        };
    }
    if (scope === "FILTERED" && (typeof filters !== "object" || filters === null)) {
        return { field: "filters", message: "Thiếu bộ lọc" };
    }
    if (typeof includeEmail !== "boolean") {
        return { field: "includeEmail", message: "Phải là true hoặc false" };
    }
    return includeEmail ? reasonFieldError(reason) : null;
}

function searchParamsOf(filters: object): URLSearchParams {
    return new URLSearchParams(
        Object.entries(filters).map(([name, value]) => [name, String(value)]),
    );
}

function exportedUsers({ scope, filters, usernames }: ExportBody, now: Date): AdminUserSummary[] {
    if (scope === "FILTERED") {
        return queryAdminUsers(searchParamsOf(filters as object), now);
    }
    const users = readAdminUsers(now);
    if (scope === "ALL") {
        return users;
    }
    return (usernames as string[]).flatMap(
        (username) => users.find((user) => user.username === username) ?? [],
    );
}

function csvOf(users: AdminUserSummary[], includeEmail: boolean): string {
    const columns = includeEmail
        ? (["username", "email", ...BASE_COLUMNS] as const)
        : (["username", ...BASE_COLUMNS] as const);
    return toCsv([columns, ...users.map((user) => columns.map((column) => user[column]))]);
}

function exportFileName(now: Date): string {
    const stamp = now.toISOString().slice(0, 16).replace(/-|:/g, "").replace("T", "-");
    return `users-${stamp}.csv`;
}

export const adminUserExportHandlers: RequestHandler[] = [
    http.post("*/api/admin/user-exports", async ({ request }) => {
        await delay();
        const body = (await request.json()) as ExportBody;
        const invalid = exportFieldError(body);
        if (invalid) {
            return validationFailed(invalid);
        }
        const now = new Date();
        return new HttpResponse(csvOf(exportedUsers(body, now), body.includeEmail === true), {
            status: 200,
            headers: {
                "Content-Type": "text/csv; charset=utf-8",
                "Content-Disposition": `attachment; filename="${exportFileName(now)}"`,
            },
        });
    }),
];
