import { useQuery } from "@tanstack/react-query";

import { profileApi } from "../api";

export function useProfile(username: string) {
    return useQuery({
        queryKey: ["profile", username],
        queryFn: ({ signal }) => profileApi.getProfile(username, signal),
        retry: false,
    });
}
