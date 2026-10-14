import { useMutation, useQueryClient } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useUploadAvatar() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: adminApi.uploadAvatar,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] }),
    });
}

export function useRemoveAvatar() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: adminApi.removeAvatar,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] }),
    });
}
