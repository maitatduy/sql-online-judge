import { useQuery } from "@tanstack/react-query";

import { profileApi } from "../api";

export function useRatingHistory(username: string) {
    return useQuery({
        queryKey: ["profile", username, "rating-history"],
        queryFn: ({ signal }) => profileApi.getRatingHistory(username, signal),
        select: (response) => response.items,
        retry: false,
    });
}
