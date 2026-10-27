import { useMutation, useQueryClient } from "@tanstack/react-query";

import { apiClient } from "@/lib/api-client";

import { invitationApi } from "../api";

export function useAcceptInvitation(token: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: () => invitationApi.accept(token),
        onSuccess: async () => {
            await apiClient.refreshSession();
            await queryClient.invalidateQueries({ queryKey: ["current-user"] });
        },
    });
}
