import { useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";

export function usePendingInvitationEmails(): ReadonlySet<string> {
    const { data } = useQuery({
        queryKey: ["admin", "role-invitations", "pending"],
        queryFn: ({ signal }) => adminApi.getPendingInvitations(signal),
        select: (page) => new Set(page.items.map((invitation) => invitation.email.toLowerCase())),
        retry: false,
    });
    return data ?? new Set();
}
