import { useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";

const ACTORS_STALE_TIME_MS = 5 * 60_000;

export function useAuditActors() {
    return useQuery({
        queryKey: ["admin", "audit-log", "actors"],
        queryFn: ({ signal }) => adminApi.getAuditActors(signal),
        staleTime: ACTORS_STALE_TIME_MS,
        retry: false,
    });
}
