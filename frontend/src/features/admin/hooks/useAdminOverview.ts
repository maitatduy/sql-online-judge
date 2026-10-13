import { useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";

const OVERVIEW_REFRESH_MS = 60_000;

export function useAdminOverview() {
    return useQuery({
        queryKey: ["admin", "overview"],
        queryFn: ({ signal }) => adminApi.getOverview(signal),
        refetchInterval: OVERVIEW_REFRESH_MS,
        retry: false,
    });
}
