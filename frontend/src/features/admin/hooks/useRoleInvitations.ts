import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";
import type { RoleInvitationFilters } from "../invitations/types";

export function useRoleInvitations(filters: RoleInvitationFilters) {
    return useQuery({
        queryKey: ["admin", "role-invitations", "list", filters],
        queryFn: ({ signal }) => adminApi.getRoleInvitations(filters, signal),
        placeholderData: keepPreviousData,
    });
}
