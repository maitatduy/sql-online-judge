import { useMutation, useQueryClient } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useDeleteUser() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: adminApi.deleteUser,
        onSuccess: (_result, { username }) => {
            queryClient.removeQueries({ queryKey: ["admin", "users", "detail", username] });
            return queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
        },
    });
}
