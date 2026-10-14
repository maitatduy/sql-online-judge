import type { Role } from "@/lib/current-user";

import type { UsersQuery } from "./users-query";

export type AccountStatus = "ACTIVE" | "BANNED";

export interface AdminUserSummary {
    id: string;
    username: string;
    email: string;
    role: Role;
    rating: number;
    acceptedCount: number;
    joinedAt: string;
    lastActiveAt: string;
    isOnline: boolean;
    status: AccountStatus;
    bannedUntil: string | null;
    isEmailVerified: boolean;
    avatarUrl: string | null;
    deletedAt: string | null;
}

export interface AdminUsersResponse {
    items: AdminUserSummary[];
    page: number;
    size: number;
    totalItems: number;
    totalPages: number;
}

export interface UserCountsResponse {
    total: number;
    unverified: number;
    setters: number;
    moderators: number;
    banned: number;
}

export type SignupProvider = "EMAIL" | "GOOGLE" | "GITHUB";
export type ViolationSeverity = "SEVERE" | "MINOR";

export interface UserViolation {
    id: string;
    severity: ViolationSeverity;
    title: string;
    detail: string;
}

export interface AdminUserDetail extends AdminUserSummary {
    displayName: string | null;
    signupProvider: SignupProvider;
    violations: UserViolation[];
}

export interface UpdateUserRequest {
    username: string;
    role?: Role;
    displayName?: string | null;
    reason?: string;
}

export type BanDuration = "DAYS_7" | "DAYS_30" | "PERMANENT";

export interface BanUserRequest {
    username: string;
    duration: BanDuration;
    reason: string;
    notifyByEmail: boolean;
}

export interface UnbanUserRequest {
    username: string;
    reason: string;
}

export interface UserEmailResponse {
    username: string;
}

export interface BulkBanUsersRequest {
    usernames: string[];
    duration: BanDuration;
    reason: string;
    notifyByEmail: boolean;
}

export interface BulkChangeRoleRequest {
    usernames: string[];
    role: Role;
    reason: string;
}

export interface BulkUserActionResponse {
    affectedCount: number;
    skippedUsernames: string[];
}

export type ExportScope = "FILTERED" | "SELECTED" | "ALL";

export interface ExportUsersRequest {
    scope: ExportScope;
    filters?: UsersQuery;
    usernames?: string[];
    includeEmail: boolean;
    reason?: string;
}

export interface ExportUsersResponse {
    exportId: string;
}

export interface DeleteUserRequest {
    username: string;
    reason: string;
}

export interface CreateUserRequest {
    username: string;
    email: string;
    role: Role;
    displayName?: string;
}

export interface UploadAvatarRequest {
    username: string;
    file: File;
}

export interface UserAvatarResponse {
    avatarUrl: string;
}
