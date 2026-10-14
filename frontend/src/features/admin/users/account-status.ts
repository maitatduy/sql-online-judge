import type { AccountStatus, AdminUserSummary } from "./types";

export type AccountState = "ACTIVE" | "BANNED" | "UNVERIFIED";

interface AccountStateQuery {
    status: AccountStatus;
    emailVerified?: boolean;
}

export const ACCOUNT_STATE_LABELS: Record<AccountState, string> = {
    ACTIVE: "Hoạt động",
    BANNED: "Bị khoá",
    UNVERIFIED: "Chưa xác minh",
};

export const ACCOUNT_STATES = Object.keys(ACCOUNT_STATE_LABELS) as AccountState[];

export const ACCOUNT_STATE_QUERIES: Record<AccountState, AccountStateQuery> = {
    ACTIVE: { status: "ACTIVE", emailVerified: true },
    BANNED: { status: "BANNED" },
    UNVERIFIED: { status: "ACTIVE", emailVerified: false },
};

export function accountStateOf({
    status,
    isEmailVerified,
}: Pick<AdminUserSummary, "status" | "isEmailVerified">): AccountState {
    if (status === "BANNED") {
        return "BANNED";
    }
    return isEmailVerified ? "ACTIVE" : "UNVERIFIED";
}
