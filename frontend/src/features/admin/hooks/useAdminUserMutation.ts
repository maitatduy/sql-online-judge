import { useMutation, useQueryClient } from "@tanstack/react-query";

import type { AdminUserDetail } from "../users/types";

export function useAdminUserMutation<TRequest>(
    mutationFn: (request: TRequest) => Promise<AdminUserDetail>,
) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn,
        onSuccess: (updatedUser) => {
            queryClient.setQueryData(
                ["admin", "users", "detail", updatedUser.username],
                updatedUser,
            );
            return queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
        },
    });
}
