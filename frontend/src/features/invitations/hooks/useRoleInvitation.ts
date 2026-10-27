import { useQuery } from "@tanstack/react-query";

import { invitationApi } from "../api";

export function useRoleInvitation(token: string) {
    return useQuery({
        queryKey: ["role-invitation", token],
        queryFn: ({ signal }) => invitationApi.view(token, signal),
        retry: false,
    });
}
