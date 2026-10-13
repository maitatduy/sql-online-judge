import { useQuery } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";
import type { CurrentUser } from "@/lib/current-user";

const CURRENT_USER_STALE_TIME_MS = 5 * 60_000;

export function useCurrentUser() {
    return useQuery({
        queryKey: ["current-user"],
        queryFn: ({ signal }) => apiClient.get<CurrentUser>("/api/auth/me", { signal }),
        staleTime: CURRENT_USER_STALE_TIME_MS,
        retry: false,
    });
}
