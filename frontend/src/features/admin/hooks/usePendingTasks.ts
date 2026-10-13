import { useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";

const PENDING_TASKS_REFRESH_MS = 60_000;

export function usePendingTasks() {
    return useQuery({
        queryKey: ["admin", "pending-tasks"],
        queryFn: ({ signal }) => adminApi.getPendingTasks(signal),
        select: (response) => response.items,
        refetchInterval: PENDING_TASKS_REFRESH_MS,
        retry: false,
    });
}
