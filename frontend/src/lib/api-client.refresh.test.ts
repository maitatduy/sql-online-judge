import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it } from "vitest";

import { server } from "@/mocks/server";

import { apiClient } from "./api-client";
import { clearAccessToken, getAccessToken, setAccessToken } from "./auth-session";

const UNAUTHORIZED = { status: 401, code: "UNAUTHORIZED", message: "Bạn cần đăng nhập" };

function protectedEndpoint(validToken: string) {
    return http.get("*/api/problems", ({ request }) =>
        request.headers.get("Authorization") === `Bearer ${validToken}`
            ? HttpResponse.json({ ok: true })
            : HttpResponse.json(UNAUTHORIZED, { status: 401 }),
    );
}

describe("apiClient - tự làm mới phiên", () => {
    afterEach(() => {
        clearAccessToken();
    });

    it("gặp 401 thì refresh bằng cookie rồi gửi lại request với token mới", async () => {
        setAccessToken("expired-token");
        server.use(
            protectedEndpoint("fresh-token"),
            http.post("*/api/auth/refresh", () =>
                HttpResponse.json({ accessToken: "fresh-token", user: {} }),
            ),
        );

        await expect(apiClient.get("/api/problems")).resolves.toEqual({ ok: true });
        expect(getAccessToken()).toBe("fresh-token");
    });

    it("nhiều request cùng gặp 401 chỉ gọi refresh một lần", async () => {
        let refreshCount = 0;
        setAccessToken("expired-token");
        server.use(
            protectedEndpoint("fresh-token"),
            http.post("*/api/auth/refresh", () => {
                refreshCount += 1;
                return HttpResponse.json({ accessToken: "fresh-token", user: {} });
            }),
        );

        await Promise.all([
            apiClient.get("/api/problems"),
            apiClient.get("/api/problems"),
            apiClient.get("/api/problems"),
        ]);

        expect(refreshCount).toBe(1);
    });

    it("refresh thất bại thì xoá token và trả lỗi 401 ban đầu", async () => {
        setAccessToken("expired-token");
        server.use(
            protectedEndpoint("fresh-token"),
            http.post("*/api/auth/refresh", () =>
                HttpResponse.json(
                    { status: 401, code: "INVALID_REFRESH_TOKEN", message: "Phiên đã hết hạn" },
                    { status: 401 },
                ),
            ),
        );

        await expect(apiClient.get("/api/problems")).rejects.toMatchObject({
            status: 401,
            code: "UNAUTHORIZED",
        });
        expect(getAccessToken()).toBeNull();
    });

    it("refresh và logout chỉ dựa vào cookie, không gửi access token", async () => {
        const authorizations: (string | null)[] = [];
        setAccessToken("expired-token");
        server.use(
            http.post("*/api/auth/refresh", ({ request }) => {
                authorizations.push(request.headers.get("Authorization"));
                return HttpResponse.json({ accessToken: "fresh-token", user: {} });
            }),
            http.post("*/api/auth/logout", ({ request }) => {
                authorizations.push(request.headers.get("Authorization"));
                return new HttpResponse(null, { status: 204 });
            }),
        );

        await apiClient.post("/api/auth/refresh");
        await apiClient.post("/api/auth/logout");

        expect(authorizations).toEqual([null, null]);
    });

    it("đăng nhập sai (401) không kích hoạt refresh", async () => {
        let refreshCount = 0;
        server.use(
            http.post("*/api/auth/login", () =>
                HttpResponse.json(
                    { status: 401, code: "INVALID_CREDENTIALS", message: "Sai thông tin" },
                    { status: 401 },
                ),
            ),
            http.post("*/api/auth/refresh", () => {
                refreshCount += 1;
                return HttpResponse.json({ accessToken: "fresh-token", user: {} });
            }),
        );

        await expect(apiClient.post("/api/auth/login", {})).rejects.toMatchObject({
            code: "INVALID_CREDENTIALS",
        });
        expect(refreshCount).toBe(0);
    });

    it("mở lại trang khi chưa có token: /me 401 thì refresh bằng cookie để giữ đăng nhập", async () => {
        server.use(
            http.get("*/api/auth/me", ({ request }) =>
                request.headers.get("Authorization") === "Bearer fresh-token"
                    ? HttpResponse.json({ username: "minh.anh" })
                    : HttpResponse.json(UNAUTHORIZED, { status: 401 }),
            ),
            http.post("*/api/auth/refresh", () =>
                HttpResponse.json({ accessToken: "fresh-token", user: {} }),
            ),
        );

        await expect(apiClient.get("/api/auth/me")).resolves.toEqual({ username: "minh.anh" });
    });
});
