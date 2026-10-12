import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AuthResponse, LoginRequest, RegisterRequest } from "@/features/auth/types";

const DEMO_EMAIL = "demo@sqljudge.dev";
const DEMO_PASSWORD = "mock-password";
const LOCKED_EMAIL = "locked@sqljudge.dev";
const TAKEN_USERNAME = "taken";
const TAKEN_EMAIL = "taken@sqljudge.dev";
const UNAVAILABLE_USERNAMES = [TAKEN_USERNAME, "admin", "demo"];
const RETRY_AFTER_SECONDS = 30;
const MOCK_USER_ID = "0192f3a0-0000-7000-8000-000000000001";

function authResponse(username: string, email: string): AuthResponse {
    return {
        accessToken: "mock-access-token",
        user: { id: MOCK_USER_ID, username, email, role: "LEARNER" },
    };
}

function apiError(status: number, code: string, message: string, headers?: HeadersInit) {
    return HttpResponse.json({ status, code, message }, { status, headers });
}

export const handlers: RequestHandler[] = [
    http.post("*/api/auth/login", async ({ request }) => {
        await delay();
        const body = (await request.json()) as LoginRequest;
        if (body.email === LOCKED_EMAIL) {
            return apiError(429, "LOGIN_RATE_LIMITED", "Bạn đã thử quá nhiều lần", {
                "Retry-After": String(RETRY_AFTER_SECONDS),
            });
        }
        if (body.email === DEMO_EMAIL && body.password === DEMO_PASSWORD) {
            return HttpResponse.json(authResponse("demo", DEMO_EMAIL));
        }
        return apiError(401, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng");
    }),

    http.post("*/api/auth/oauth/:provider", async ({ params }) => {
        await delay();
        const provider = String(params.provider);
        return HttpResponse.json(authResponse(`${provider}.demo`, `${provider}@sqljudge.dev`));
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
        return HttpResponse.json(authResponse(body.username, body.email), { status: 201 });
    }),

    http.get("*/api/users/availability", async ({ request }) => {
        await delay();
        const username = new URL(request.url).searchParams.get("username") ?? "";
        return HttpResponse.json({ available: !UNAVAILABLE_USERNAMES.includes(username) });
    }),
];
