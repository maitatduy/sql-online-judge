import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";
import type { PageRequest } from "../users/types";

export function useUserContestHistory(username: string, pageRequest: PageRequest) {
    return useQuery({
        queryKey: ["admin", "users", "contests", username, pageRequest],
        queryFn: ({ signal }) => adminApi.getUserContestHistory(username, pageRequest, signal),
        placeholderData: keepPreviousData,
        retry: false,
    });
}
