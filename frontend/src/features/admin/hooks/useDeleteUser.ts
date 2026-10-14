import { useMutation, useQueryClient } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useDeleteUser() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: adminApi.deleteUser,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] }),
    });
}
