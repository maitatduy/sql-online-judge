import { useQuery } from "@tanstack/react-query";

import { profileApi } from "../api";

export function useProfile(username: string) {
    return useQuery({
        queryKey: ["profile", username],
        queryFn: ({ signal }) => profileApi.getProfile(username, signal),
        retry: false,
    });
}

export function useRatingSummary(userId: string | undefined) {
    return useQuery({
        queryKey: ["profile", "rating", userId],
        queryFn: ({ signal }) => profileApi.getRatingSummary(userId ?? "", signal),
        enabled: userId !== undefined,
        retry: false,
    });
}

export function useSolvedSummary(userId: string | undefined) {
    return useQuery({
        queryKey: ["profile", "solved", userId],
        queryFn: ({ signal }) => profileApi.getSolvedSummary(userId ?? "", signal),
        enabled: userId !== undefined,
        retry: false,
    });
}
