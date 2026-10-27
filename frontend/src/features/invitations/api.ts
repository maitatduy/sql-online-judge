import { apiClient } from "@/lib/api-client";

import type { AcceptInvitationResponse, RoleInvitationView } from "./types";

function invitationPath(token: string): string {
    return `/api/role-invitations/${encodeURIComponent(token)}`;
}

export const invitationApi = {
    view: (token: string, signal?: AbortSignal) =>
        apiClient.get<RoleInvitationView>(invitationPath(token), { signal }),
    accept: (token: string) =>
        apiClient.post<AcceptInvitationResponse>(`${invitationPath(token)}/accept`),
};
