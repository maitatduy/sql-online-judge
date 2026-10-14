import type { Role } from "@/lib/current-user";

import { ACCOUNT_STATE_QUERIES } from "./account-status";
import type { UsersFilters } from "./search-params";
import type { AccountStatus } from "./types";

export interface UsersQuery {
    q?: string;
    role?: Role;
    status?: AccountStatus;
    emailVerified?: boolean;
    deleted?: boolean;
}

export function usersQueryOf({ q, role, state }: UsersFilters): UsersQuery {
    return {
        ...(q ? { q } : {}),
        ...(role ? { role } : {}),
        ...(state ? ACCOUNT_STATE_QUERIES[state] : {}),
    };
}
