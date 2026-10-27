import { useMutation, useQueryClient } from "@tanstack/react-query";

import { adminApi } from "../api";

function useInvitationMutation<T>(mutationFn: (id: string) => Promise<T>) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "role-invitations"] }),
    });
}

export function useRevokeInvitation() {
    return useInvitationMutation(adminApi.revokeInvitation);
}

export function useResendInvitation() {
    return useInvitationMutation(adminApi.resendInvitation);
}
