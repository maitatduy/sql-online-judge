import type { AdminNavigationCounts } from "@/lib/admin-navigation";
import { apiClient } from "@/lib/api-client";

import type {
    AdminOverviewResponse,
    JudgeWorkersResponse,
    PendingTasksResponse,
    RestartWorkerRequest,
    RestartWorkerResponse,
} from "./types";
import { ACCOUNT_STATE_QUERIES } from "./users/account-status";
import { USERS_PAGE_SIZE } from "./users/pagination";
import type { UsersFilters } from "./users/search-params";
import { formatUsersSort } from "./users/sort";
import type {
    AdminUserDetail,
    AdminUsersResponse,
    BanUserRequest,
    ChangeUserRoleRequest,
    UnbanUserRequest,
    UserCountsResponse,
    UserEmailResponse,
} from "./users/types";

export const adminApi = {
    getUsers: ({ q, role, state, sort, page }: UsersFilters, signal?: AbortSignal) =>
        apiClient.get<AdminUsersResponse>("/api/admin/users", {
            params: {
                q: q || undefined,
                role: role ?? undefined,
                ...(state ? ACCOUNT_STATE_QUERIES[state] : {}),
                sort: sort ? formatUsersSort(sort) : undefined,
                page,
                size: USERS_PAGE_SIZE,
            },
            signal,
        }),
    getUserCounts: (signal?: AbortSignal) =>
        apiClient.get<UserCountsResponse>("/api/admin/users/summary", { signal }),
    getUser: (username: string, signal?: AbortSignal) =>
        apiClient.get<AdminUserDetail>(`/api/admin/users/${encodeURIComponent(username)}`, {
            signal,
        }),
    changeUserRole: ({ username, role }: ChangeUserRoleRequest) =>
        apiClient.patch<AdminUserDetail>(`/api/admin/users/${encodeURIComponent(username)}`, {
            role,
        }),
    banUser: ({ username, ...ban }: BanUserRequest) =>
        apiClient.post<AdminUserDetail>(
            `/api/admin/users/${encodeURIComponent(username)}/ban`,
            ban,
        ),
    unbanUser: ({ username, reason }: UnbanUserRequest) =>
        apiClient.post<AdminUserDetail>(`/api/admin/users/${encodeURIComponent(username)}/unban`, {
            reason,
        }),
    sendPasswordResetEmail: (username: string) =>
        apiClient.post<UserEmailResponse>(
            `/api/admin/users/${encodeURIComponent(username)}/password-reset-email`,
        ),
    resendVerificationEmail: (username: string) =>
        apiClient.post<UserEmailResponse>(
            `/api/admin/users/${encodeURIComponent(username)}/verification-email`,
        ),
    getNavigationCounts: (signal?: AbortSignal) =>
        apiClient.get<AdminNavigationCounts>("/api/admin/navigation-counts", { signal }),
    getOverview: (signal?: AbortSignal) =>
        apiClient.get<AdminOverviewResponse>("/api/admin/overview", { signal }),
    getJudgeWorkers: (signal?: AbortSignal) =>
        apiClient.get<JudgeWorkersResponse>("/api/admin/judge/workers", { signal }),
    restartWorker: ({ name, reason }: RestartWorkerRequest) =>
        apiClient.post<RestartWorkerResponse>(
            `/api/admin/judge/workers/${encodeURIComponent(name)}/restart`,
            { reason },
        ),
    getPendingTasks: (signal?: AbortSignal) =>
        apiClient.get<PendingTasksResponse>("/api/admin/pending-tasks", { signal }),
};
