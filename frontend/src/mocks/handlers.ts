import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AuthResponse, LoginRequest, RegisterRequest } from "@/features/auth/types";
import type { CurrentUser } from "@/lib/current-user";

import { apiError } from "./api-error";
import { DEMO_SESSION_USER, readMockSession, writeMockSession } from "./session";

const DEMO_EMAIL = "demo@sqljudge.dev";
const DEMO_PASSWORD = "mock-password";
const LOCKED_EMAIL = "locked@sqljudge.dev";
const TAKEN_USERNAME = "taken";
const TAKEN_EMAIL = "taken@sqljudge.dev";
const UNAVAILABLE_USERNAMES = [TAKEN_USERNAME, "admin", "demo"];
const RETRY_AFTER_SECONDS = 30;
const NEW_USER_ID = "0192f3a0-0000-7000-8000-000000000002";

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
        if (body.email === DEMO_EMAIL && body.password === DEMO_PASSWORD) {
            return HttpResponse.json(signIn(DEMO_SESSION_USER, DEMO_EMAIL));
        }
        return apiError(401, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng");
    }),

    http.get("*/api/auth/me", async () => {
        await delay();
        const currentUser = readMockSession();
        if (!currentUser) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        return HttpResponse.json(currentUser);
    }),

    http.post("*/api/auth/oauth/:provider", async ({ params }) => {
        await delay();
        const provider = String(params.provider);
        return HttpResponse.json(
            signIn(newSessionUser(`${provider}.demo`), `${provider}@sqljudge.dev`),
        );
    }),

    http.post("*/api/auth/register", async ({ request }) => {
        await delay();
        const body = (await request.json()) as RegisterRequest;
        if (body.username === TAKEN_USERNAME) {
            return apiError(409, "USERNAME_TAKEN", "Tên người dùng đã có người dùng");
        }
        if (body.email === TAKEN_EMAIL) {
            return apiError(409, "EMAIL_TAKEN", "Email đã được đăng ký");
        }
        return HttpResponse.json(signIn(newSessionUser(body.username), body.email), {
            status: 201,
        });
    }),

    http.get("*/api/users/availability", async ({ request }) => {
        await delay();
        const username = new URL(request.url).searchParams.get("username") ?? "";
        return HttpResponse.json({ available: !UNAVAILABLE_USERNAMES.includes(username) });
    }),
];
