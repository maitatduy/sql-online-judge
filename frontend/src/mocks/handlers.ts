import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    AuthResponse,
    ConfirmEmailRequest,
    LoginRequest,
    RegisterRequest,
    RegisterResponse,
    ResetPasswordRequest,
} from "@/features/auth/types";
import type { CurrentUser } from "@/lib/current-user";

import { adminHandlers } from "./admin-handlers";
import { apiError } from "./api-error";
import { invitationHandlers } from "./invitation-handlers";
import { practiceHandlers } from "./practice-handlers";
import { practiceRunHandlers } from "./practice-run-handlers";
import { practiceSubmissionHandlers } from "./practice-submission-handlers";
import { profileHandlers } from "./profile-handlers";
import {
    ADMIN_SESSION_USER,
    clearMockSession,
    DEMO_SESSION_USER,
    readMockSession,
    writeMockSession,
} from "./session";

const DEMO_PASSWORD = "mock-password";
const DEMO_ACCOUNTS: Record<string, CurrentUser> = {
    "demo@sqljudge.dev": DEMO_SESSION_USER,
    "admin@sqljudge.dev": ADMIN_SESSION_USER,
};
const LOCKED_EMAIL = "locked@sqljudge.dev";
const UNVERIFIED_EMAIL = "unverified@sqljudge.dev";
const TAKEN_USERNAME = "taken";
const UNAVAILABLE_USERNAMES = [TAKEN_USERNAME, "admin", "demo"];
const RETRY_AFTER_SECONDS = 30;
const NEW_USER_ID = "0192f3a0-0000-7000-8000-000000000002";
const VALID_CODE = "482913";
const VALID_LINK_TOKEN = "valid-link-token";
const VALID_RESET_TOKEN = "valid-reset-token";

function newSessionUser(username: string): CurrentUser {
    return {
        id: NEW_USER_ID,
        username,
        displayName: username,
        role: "LEARNER",
        currentStreakDays: 0,
    };
}

function signIn(user: CurrentUser, email: string): AuthResponse {
    writeMockSession(user);
    return {
        accessToken: "mock-access-token",
        user: { id: user.id, username: user.username, email, role: user.role },
    };
}

export const handlers: RequestHandler[] = [
    http.post("*/api/auth/login", async ({ request }) => {
        await delay();
        const body = (await request.json()) as LoginRequest;
        if (body.email === LOCKED_EMAIL) {
            return apiError(429, "LOGIN_RATE_LIMITED", "Bạn đã thử quá nhiều lần", {
                headers: {
                    "Retry-After": String(RETRY_AFTER_SECONDS),
                },
            });
        }
        if (body.email === UNVERIFIED_EMAIL) {
            return apiError(
                403,
                "EMAIL_NOT_VERIFIED",
                "Email chưa được xác thực. Hãy nhập mã đã gửi tới email của bạn",
            );
        }
        const demoUser = DEMO_ACCOUNTS[body.email];
        if (demoUser && body.password === DEMO_PASSWORD) {
            return HttpResponse.json(signIn(demoUser, body.email));
        }
        return apiError(401, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng");
    }),

    http.post("*/api/auth/refresh", async () => {
        await delay();
        const currentUser = readMockSession();
        if (!currentUser) {
            return apiError(
                401,
                "INVALID_REFRESH_TOKEN",
                "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại",
            );
        }
        return HttpResponse.json(signIn(currentUser, `${currentUser.username}@sqljudge.dev`));
    }),

    http.post("*/api/auth/logout", async () => {
        await delay();
        clearMockSession();
        return new HttpResponse(null, { status: 204 });
    }),

    http.get("*/api/auth/me", async () => {
        await delay();
        const currentUser = readMockSession();
        if (!currentUser) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        return HttpResponse.json(currentUser);
    }),

    http.post("*/api/auth/register", async ({ request }) => {
        await delay();
        const body = (await request.json()) as RegisterRequest;
        if (body.username === TAKEN_USERNAME) {
            return apiError(409, "USERNAME_TAKEN", "Tên người dùng đã có người dùng");
        }
        return HttpResponse.json<RegisterResponse>({ email: body.email }, { status: 201 });
    }),

    http.post("*/api/auth/email-verification/confirm", async ({ request }) => {
        await delay();
        const body = (await request.json()) as ConfirmEmailRequest;
        const isValid =
            "token" in body ? body.token === VALID_LINK_TOKEN : body.code === VALID_CODE;
        if (!isValid) {
            return apiError(422, "TOKEN_INVALID_OR_EXPIRED", "Mã không hợp lệ hoặc đã hết hạn");
        }
        const email = "email" in body ? body.email : "verified@sqljudge.dev";
        return HttpResponse.json(signIn(newSessionUser(email.split("@")[0]), email));
    }),

    http.post("*/api/auth/email-verification", async () => {
        await delay();
        return new HttpResponse(null, { status: 202 });
    }),

    http.post("*/api/auth/password-reset", async () => {
        await delay();
        return new HttpResponse(null, { status: 202 });
    }),

    http.post("*/api/auth/password-reset/confirm", async ({ request }) => {
        await delay();
        const body = (await request.json()) as ResetPasswordRequest;
        if (body.token !== VALID_RESET_TOKEN) {
            return apiError(
                422,
                "TOKEN_INVALID_OR_EXPIRED",
                "Liên kết không hợp lệ hoặc đã hết hạn",
            );
        }
        return new HttpResponse(null, { status: 204 });
    }),

    http.get("*/api/users/availability", async ({ request }) => {
        await delay();
        const username = new URL(request.url).searchParams.get("username") ?? "";
        return HttpResponse.json({ available: !UNAVAILABLE_USERNAMES.includes(username) });
    }),

    ...profileHandlers,
    ...practiceHandlers,
    ...practiceRunHandlers,
    ...practiceSubmissionHandlers,
    ...adminHandlers,
    ...invitationHandlers,
];
