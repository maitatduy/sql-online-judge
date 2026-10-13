import { describe, expect, it } from "vitest";

import { apiClient } from "@/lib/api-client";
import type { CurrentUser } from "@/lib/current-user";

import { DEMO_SESSION_USER, writeMockSession } from "./session";

describe("API quản trị giả", () => {
    it("đăng nhập bằng tài khoản quản trị trả về vai trò ADMIN", async () => {
        await apiClient.post("/api/auth/login", {
            email: "admin@sqljudge.dev",
            password: "mock-password",
            rememberMe: false,
        });

        const currentUser = await apiClient.get<CurrentUser>("/api/auth/me");

        expect(currentUser).toMatchObject({ username: "admin.huy", role: "ADMIN" });
    });

    it("trả 401 cho mọi đường dẫn quản trị khi chưa đăng nhập", async () => {
        await expect(apiClient.get("/api/admin/overview")).rejects.toMatchObject({
            status: 401,
            code: "UNAUTHENTICATED",
        });
    });

    it("trả 403 cho mọi đường dẫn quản trị khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/judge/workers/judge-my-02/restart", { reason: "Thử" }),
        ).rejects.toMatchObject({ status: 403, code: "ACCESS_DENIED" });
    });
});
