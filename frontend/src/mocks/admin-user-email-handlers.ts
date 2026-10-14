import { delay, http, HttpResponse, type RequestHandler } from "msw";

import { userNotFound } from "./admin-user-errors";
import { findAdminUser } from "./admin-user-store";
import { apiError } from "./api-error";

const ACCEPTED_STATUS = 202;

export const adminUserEmailHandlers: RequestHandler[] = [
    http.post("*/api/admin/users/:username/password-reset-email", async ({ params }) => {
        await delay();
        const user = findAdminUser(String(params.username));
        if (!user) {
            return userNotFound();
        }
        return HttpResponse.json({ username: user.username }, { status: ACCEPTED_STATUS });
    }),

    http.post("*/api/admin/users/:username/verification-email", async ({ params }) => {
        await delay();
        const user = findAdminUser(String(params.username));
        if (!user) {
            return userNotFound();
        }
        if (user.isEmailVerified) {
            return apiError(409, "EMAIL_ALREADY_VERIFIED", "Email đã được xác minh");
        }
        return HttpResponse.json({ username: user.username }, { status: ACCEPTED_STATUS });
    }),
];
