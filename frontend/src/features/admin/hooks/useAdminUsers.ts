import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";
import type { UsersFilters } from "../users/search-params";

export function useAdminUsers(filters: UsersFilters) {
    return useQuery({
        queryKey: ["admin", "users", "list", filters],
        queryFn: ({ signal }) => adminApi.getUsers(filters, signal),
        placeholderData: keepPreviousData,
        retry: false,
    });
}
