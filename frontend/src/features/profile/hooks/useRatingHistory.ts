import { useQuery } from "@tanstack/react-query";

import { profileApi } from "../api";

export function useRatingHistory(userId: string) {
    return useQuery({
        queryKey: ["profile", userId, "rating-history"],
        queryFn: ({ signal }) => profileApi.getRatingHistory(userId, signal),
        select: (response) => response.items,
        retry: false,
    });
}
