import type { Role } from "@/lib/current-user";
import { ROUTES } from "@/lib/routes";

import { ACCOUNT_STATES, type AccountState } from "./account-status";
import { ROLES } from "./role-labels";
import { formatUsersSort, parseUsersSort, type UsersSort } from "./sort";

const QUERY_PARAM = "q";
const ROLE_PARAM = "role";
const STATE_PARAM = "status";
const SORT_PARAM = "sort";
const PAGE_PARAM = "page";
const OPEN_USER_PARAM = "user";
const FIRST_PAGE = 1;

export interface UsersFilters {
    q: string;
    role: Role | null;
    state: AccountState | null;
    sort: UsersSort | null;
    page: number;
    openUsername: string | null;
}

export const NO_USERS_FILTERS: UsersFilters = {
    q: "",
    role: null,
    state: null,
    sort: null,
    page: FIRST_PAGE,
    openUsername: null,
};

function knownValue<T extends string>(knownValues: T[], value: string | null): T | null {
    return knownValues.find((knownValue) => knownValue === value) ?? null;
}

function readPage(value: string | null): number {
    const page = Number(value);
    return Number.isInteger(page) && page >= FIRST_PAGE ? page : FIRST_PAGE;
}

export function readUsersFilters(searchParams: Pick<URLSearchParams, "get">): UsersFilters {
    return {
        q: searchParams.get(QUERY_PARAM)?.trim() ?? "",
        role: knownValue(ROLES, searchParams.get(ROLE_PARAM)),
        state: knownValue(ACCOUNT_STATES, searchParams.get(STATE_PARAM)),
        sort: parseUsersSort(searchParams.get(SORT_PARAM)),
        page: readPage(searchParams.get(PAGE_PARAM)),
        openUsername: searchParams.get(OPEN_USER_PARAM) || null,
    };
}

export function usersHref({ q, role, state, sort, page, openUsername }: UsersFilters): string {
    const query = new URLSearchParams();
    if (q) {
        query.set(QUERY_PARAM, q);
    }
    if (role) {
        query.set(ROLE_PARAM, role);
    }
    if (state) {
        query.set(STATE_PARAM, state);
    }
    if (sort) {
        query.set(SORT_PARAM, formatUsersSort(sort));
    }
    if (page > FIRST_PAGE) {
        query.set(PAGE_PARAM, String(page));
    }
    if (openUsername) {
        query.set(OPEN_USER_PARAM, openUsername);
    }
    const queryString = query.toString();
    return queryString ? `${ROUTES.adminUsers}?${queryString}` : ROUTES.adminUsers;
}

export function hasActiveFilter({ q, role, state }: UsersFilters): boolean {
    return q !== "" || role !== null || state !== null;
}
