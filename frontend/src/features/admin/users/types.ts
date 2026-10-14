import type { Role } from "@/lib/current-user";

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

export interface ChangeUserRoleRequest {
    username: string;
    role: Role;
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
