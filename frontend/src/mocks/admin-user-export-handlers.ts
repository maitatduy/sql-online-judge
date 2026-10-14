import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { ExportUsersResponse } from "@/features/admin/users/types";
import type { ApiFieldError } from "@/lib/api-client";

import { reasonFieldError, validationFailed } from "./admin-user-errors";

const EXPORT_SCOPES = ["FILTERED", "SELECTED", "ALL"];
const MAX_EXPORT_USERNAMES = 1000;
const ACCEPTED_STATUS = 202;

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

export const adminUserExportHandlers: RequestHandler[] = [
    http.post("*/api/admin/user-exports", async ({ request }) => {
        await delay();
        const invalid = exportFieldError((await request.json()) as ExportBody);
        if (invalid) {
            return validationFailed(invalid);
        }
        const response: ExportUsersResponse = { exportId: `export-${Date.now()}` };
        return HttpResponse.json(response, { status: ACCEPTED_STATUS });
    }),
];
