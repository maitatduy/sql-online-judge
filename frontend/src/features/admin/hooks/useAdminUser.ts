import { useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useAdminUser(username: string) {
    return useQuery({
        queryKey: ["admin", "users", "detail", username],
        queryFn: ({ signal }) => adminApi.getUser(username, signal),
        retry: false,
    });
}
