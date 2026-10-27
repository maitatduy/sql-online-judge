import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    InviteRolesRequest,
    InviteRolesResponse,
    RoleInvitationsResponse,
} from "@/features/admin/invitations/types";

export const adminRoleInvitationHandlers: RequestHandler[] = [
    http.get("*/api/admin/role-invitations", async ({ request }) => {
        await delay();
        const size = Number(new URL(request.url).searchParams.get("size") ?? 20);
        const response: RoleInvitationsResponse = {
            items: [],
            page: 1,
            size,
            totalItems: 0,
            totalPages: 0,
        };
        return HttpResponse.json(response);
    }),

    http.post("*/api/admin/role-invitations", async ({ request }) => {
        await delay();
        const body = (await request.json()) as InviteRolesRequest;
        const response: InviteRolesResponse = {
            sentCount: new Set(body.emails.map((email) => email.toLowerCase())).size,
            skipped: [],
        };
        return HttpResponse.json(response);
    }),
];
