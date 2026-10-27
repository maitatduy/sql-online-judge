import { useMutation, useQueryClient } from "@tanstack/react-query";

import { adminApi } from "../api";

export function useInviteRoles() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: adminApi.inviteRoles,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "role-invitations"] }),
    });
}
