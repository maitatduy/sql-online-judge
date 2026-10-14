import type { AccountStatus, AdminUserSummary } from "./types";

export type AccountState = "ACTIVE" | "BANNED" | "UNVERIFIED" | "DELETED";

interface AccountStateQuery {
    status?: AccountStatus;
    emailVerified?: boolean;
    deleted?: boolean;
}

export const ACCOUNT_STATE_LABELS: Record<AccountState, string> = {
    ACTIVE: "Hoạt động",
    BANNED: "Bị khoá",
    UNVERIFIED: "Chưa xác minh",
    DELETED: "Đã xoá",
};

export const ACCOUNT_STATES = Object.keys(ACCOUNT_STATE_LABELS) as AccountState[];

export const ACCOUNT_STATE_QUERIES: Record<AccountState, AccountStateQuery> = {
    ACTIVE: { status: "ACTIVE", emailVerified: true },
    BANNED: { status: "BANNED" },
    UNVERIFIED: { status: "ACTIVE", emailVerified: false },
    DELETED: { deleted: true },
};

export function accountStateOf({
    status,
    isEmailVerified,
    deletedAt,
}: Pick<AdminUserSummary, "status" | "isEmailVerified" | "deletedAt">): AccountState {
    if (deletedAt) {
        return "DELETED";
    }
    if (status === "BANNED") {
        return "BANNED";
    }
    return isEmailVerified ? "ACTIVE" : "UNVERIFIED";
}
