import { useMutation, useQueryClient } from "@tanstack/react-query";

import { invitationApi } from "../api";

export function useDeclineInvitation(token: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: () => invitationApi.decline(token),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ["role-invitation", token] }),
    });
}
