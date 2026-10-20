import type { AdminUserSummary } from "@/features/admin/users/types";

import { readAdminUsers, readDeletedAdminUsers } from "./admin-user-store";

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

export function queryAdminUsers(query: URLSearchParams, now: Date): AdminUserSummary[] {
    const source = query.get("deleted") === "true" ? readDeletedAdminUsers : readAdminUsers;
    return source(now)
        .filter((user) => matchesQuery(user, query))
        .sort((first, second) => second.joinedAt.localeCompare(first.joinedAt));
}
