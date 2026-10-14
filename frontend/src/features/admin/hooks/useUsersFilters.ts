import { useSearchParams } from "next/navigation";

import {
    NO_USERS_FILTERS,
    readUsersFilters,
    usersHref,
    type UsersFilters,
} from "../users/search-params";

export type UsersFilterChange = Partial<Omit<UsersFilters, "page" | "openUsername">>;

function replaceUrl(filters: UsersFilters): void {
    window.history.replaceState(null, "", usersHref(filters));
}

function currentFilters(): UsersFilters {
    return readUsersFilters(new URLSearchParams(window.location.search));
}

function changeFilters(change: UsersFilterChange): void {
    replaceUrl({ ...currentFilters(), ...change, page: NO_USERS_FILTERS.page });
}

function clearFilters(): void {
    const { sort, openUsername } = currentFilters();
    replaceUrl({ ...NO_USERS_FILTERS, sort, openUsername });
}

function openUser(username: string): void {
    replaceUrl({ ...currentFilters(), openUsername: username });
}

function closeUser(): void {
    replaceUrl({ ...currentFilters(), openUsername: null });
}

export function useUsersFilters() {
    const filters = readUsersFilters(useSearchParams());

    return { filters, changeFilters, clearFilters, openUser, closeUser };
}
