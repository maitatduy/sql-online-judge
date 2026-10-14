import { useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useUserCounts() {
    return useQuery({
        queryKey: ["admin", "users", "summary"],
        queryFn: ({ signal }) => adminApi.getUserCounts(signal),
        retry: false,
    });
}
