import { useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";

export function usePendingInvitationCount(): number | null {
    const { data } = useQuery({
        queryKey: ["admin", "role-invitations", "pending"],
        queryFn: ({ signal }) => adminApi.getPendingInvitations(signal),
        select: (page) => page.totalItems,
        retry: false,
    });
    return data ?? null;
}
