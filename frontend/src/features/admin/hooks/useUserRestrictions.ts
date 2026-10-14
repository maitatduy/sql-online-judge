import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { adminApi } from "../api";

const restrictionsKey = (username: string) => ["admin", "users", "restrictions", username];

export function useUserRestrictions(username: string) {
    return useQuery({
        queryKey: restrictionsKey(username),
        queryFn: ({ signal }) => adminApi.getUserRestrictions(username, signal),
        retry: false,
    });
}

export function useUpdateRestrictions(username: string) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: adminApi.updateUserRestrictions,
        onSuccess: (restrictions) =>
            queryClient.setQueryData(restrictionsKey(username), restrictions),
    });
}
