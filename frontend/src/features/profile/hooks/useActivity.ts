import { useQuery } from "@tanstack/react-query";

import { profileApi } from "../api";

export function useActivity(username: string) {
    return useQuery({
        queryKey: ["profile", username, "activity"],
        queryFn: ({ signal }) => profileApi.getActivity(username, signal),
        retry: false,
    });
}
