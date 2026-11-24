import { useQuery } from "@tanstack/react-query";

import { profileApi } from "../api";

export function useActivity(userId: string) {
    return useQuery({
        queryKey: ["profile", userId, "activity"],
        queryFn: ({ signal }) => profileApi.getActivity(userId, signal),
        retry: false,
    });
}
