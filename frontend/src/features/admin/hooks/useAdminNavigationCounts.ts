import { useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";

const NAVIGATION_COUNTS_REFRESH_MS = 60_000;

export function useAdminNavigationCounts() {
    return useQuery({
        queryKey: ["admin", "navigation-counts"],
        queryFn: ({ signal }) => adminApi.getNavigationCounts(signal),
        refetchInterval: NAVIGATION_COUNTS_REFRESH_MS,
        retry: false,
    });
}
