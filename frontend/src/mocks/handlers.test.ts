import { describe, expect, it } from "vitest";

import type { RegisterResponse } from "@/features/auth/types";
import { apiClient } from "@/lib/api-client";
import type { CurrentUser } from "@/lib/current-user";

describe("phiên đăng nhập giả", () => {
    it("trả 401 khi chưa đăng nhập", async () => {
        await expect(apiClient.get("/api/auth/me")).rejects.toMatchObject({
            status: 401,
            code: "UNAUTHENTICATED",
        });
    });

    it("trả người dùng mẫu sau khi đăng nhập bằng tài khoản demo", async () => {
        await apiClient.post("/api/auth/login", {
            email: "demo@sqljudge.dev",
            password: "mock-password",
            rememberMe: false,
        });

        const currentUser = await apiClient.get<CurrentUser>("/api/auth/me");

        expect(currentUser).toMatchObject({
            username: "minh.anh",
            displayName: "Nguyễn Minh Anh",
            currentStreakDays: 12,
        });
    });

    it("đăng ký chỉ trả email cần xác thực và chưa đăng nhập", async () => {
        const response = await apiClient.post<RegisterResponse>("/api/auth/register", {
            username: "thu.ha",
            email: "thu.ha@sqljudge.dev",
            password: "mock-password",
            acceptedTerms: true,
        });

        expect(response).toEqual({ email: "thu.ha@sqljudge.dev" });
        await expect(apiClient.get("/api/auth/me")).rejects.toMatchObject({ status: 401 });
    });

    it("vẫn chưa đăng nhập khi sai mật khẩu", async () => {
        await apiClient
            .post("/api/auth/login", {
                email: "demo@sqljudge.dev",
                password: "wrong",
                rememberMe: false,
            })
            .catch(() => undefined);

        await expect(apiClient.get("/api/auth/me")).rejects.toMatchObject({ status: 401 });
    });
});
