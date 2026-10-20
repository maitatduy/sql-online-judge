import type { AdminNavigationCounts } from "@/lib/admin-navigation";
import { apiClient } from "@/lib/api-client";

import type {
    AdminOverviewResponse,
    JudgeWorkersResponse,
    PendingTasksResponse,
    RestartWorkerRequest,
    RestartWorkerResponse,
} from "./types";
import { USERS_PAGE_SIZE } from "./users/pagination";
import type { UsersFilters } from "./users/search-params";
import type {
    AddUserNoteRequest,
    AdminUserDetail,
    AdminUsersResponse,
    BanUserRequest,
    BulkBanUsersRequest,
    BulkChangeRoleRequest,
    BulkUserActionResponse,
    ContestHistoryResponse,
    CreateUserRequest,
    DeleteUserRequest,
    ExportUsersRequest,
    UnbanUserRequest,
    PageRequest,
    RevokeSessionRequest,
    UpdateRestrictionsRequest,
    UpdateUserRequest,
    UploadAvatarRequest,
    UserAvatarResponse,
    UserCountsResponse,
    UserEmailResponse,
    UserNote,
    UserNotesResponse,
    UserRestrictions,
    UserSessionsResponse,
} from "./users/types";
import { usersQueryOf } from "./users/users-query";

export const adminApi = {
    getUsers: (filters: UsersFilters, signal?: AbortSignal) =>
        apiClient.get<AdminUsersResponse>("/api/admin/users", {
            params: {
                ...usersQueryOf(filters),
                page: filters.page,
                size: USERS_PAGE_SIZE,
            },
            signal,
        }),
    getUserCounts: (signal?: AbortSignal) =>
        apiClient.get<UserCountsResponse>("/api/admin/user-summary", { signal }),
    getUser: (username: string, signal?: AbortSignal) =>
        apiClient.get<AdminUserDetail>(`/api/admin/users/${encodeURIComponent(username)}`, {
            signal,
        }),
    updateUser: ({ username, ...changes }: UpdateUserRequest) =>
        apiClient.patch<AdminUserDetail>(
            `/api/admin/users/${encodeURIComponent(username)}`,
            changes,
        ),
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
    bulkBanUsers: (request: BulkBanUsersRequest) =>
        apiClient.post<BulkUserActionResponse>("/api/admin/users/bulk-ban", request),
    bulkChangeRole: (request: BulkChangeRoleRequest) =>
        apiClient.post<BulkUserActionResponse>("/api/admin/users/bulk-role", request),
    exportUsers: (request: ExportUsersRequest) =>
        apiClient.postForFile("/api/admin/user-exports", request),
    deleteUser: ({ username, reason }: DeleteUserRequest) =>
        apiClient.delete<undefined>(`/api/admin/users/${encodeURIComponent(username)}`, {
            reason,
        }),
    createUser: (request: CreateUserRequest) =>
        apiClient.post<AdminUserDetail>("/api/admin/users", request),
    uploadAvatar: ({ username, file }: UploadAvatarRequest) => {
        const form = new FormData();
        form.append("file", file);
        return apiClient.putForm<UserAvatarResponse>(
            `/api/admin/users/${encodeURIComponent(username)}/avatar`,
            form,
        );
    },
    removeAvatar: (username: string) =>
        apiClient.delete<undefined>(`/api/admin/users/${encodeURIComponent(username)}/avatar`),
    restoreUser: (username: string) =>
        apiClient.post<AdminUserDetail>(`/api/admin/users/${encodeURIComponent(username)}/restore`),
    getUserContestHistory: (username: string, { page, size }: PageRequest, signal?: AbortSignal) =>
        apiClient.get<ContestHistoryResponse>(
            `/api/admin/users/${encodeURIComponent(username)}/contest-history`,
            { params: { page, size }, signal },
        ),
    getUserSessions: (username: string, signal?: AbortSignal) =>
        apiClient.get<UserSessionsResponse>(
            `/api/admin/users/${encodeURIComponent(username)}/sessions`,
            { signal },
        ),
    revokeUserSession: ({ username, sessionId }: RevokeSessionRequest) =>
        apiClient.delete<undefined>(
            `/api/admin/users/${encodeURIComponent(username)}/sessions/${encodeURIComponent(sessionId)}`,
        ),
    revokeAllUserSessions: (username: string) =>
        apiClient.delete<undefined>(`/api/admin/users/${encodeURIComponent(username)}/sessions`),
    getUserRestrictions: (username: string, signal?: AbortSignal) =>
        apiClient.get<UserRestrictions>(
            `/api/admin/users/${encodeURIComponent(username)}/restrictions`,
            { signal },
        ),
    updateUserRestrictions: ({ username, ...changes }: UpdateRestrictionsRequest) =>
        apiClient.patch<UserRestrictions>(
            `/api/admin/users/${encodeURIComponent(username)}/restrictions`,
            changes,
        ),
    getUserNotes: (username: string, signal?: AbortSignal) =>
        apiClient.get<UserNotesResponse>(`/api/admin/users/${encodeURIComponent(username)}/notes`, {
            signal,
        }),
    addUserNote: ({ username, body }: AddUserNoteRequest) =>
        apiClient.post<UserNote>(`/api/admin/users/${encodeURIComponent(username)}/notes`, {
            body,
        }),
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
