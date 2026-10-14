import { describe, expect, it } from "vitest";

import type {
    AdminUserDetail,
    AdminUsersResponse,
    UserCountsResponse,
} from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const USERS_PATH = "/api/admin/users";
const NEW_USER = {
    username: "lan.pham",
    email: "lan.pham@example.com",
    displayName: "Phạm Ngọc Lan",
    role: "SETTER",
};

describe("POST /api/admin/users", () => {
    it("tạo tài khoản chưa xác minh và trả chi tiết", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const user = await apiClient.post<AdminUserDetail>(USERS_PATH, NEW_USER);

        expect(user).toMatchObject({
            username: "lan.pham",
            email: "lan.pham@example.com",
            displayName: "Phạm Ngọc Lan",
            role: "SETTER",
            status: "ACTIVE",
            isEmailVerified: false,
            rating: 0,
            acceptedCount: 0,
            signupProvider: "EMAIL",
            violations: [],
        });
    });

    it("đưa tài khoản mới lên đầu danh sách và cộng vào số liệu", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.post(USERS_PATH, NEW_USER);

        const list = await apiClient.get<AdminUsersResponse>(USERS_PATH);
        const counts = await apiClient.get<UserCountsResponse>("/api/admin/users/summary");
        const detail = await apiClient.get<AdminUserDetail>("/api/admin/users/lan.pham");

        expect(list.items[0].username).toBe("lan.pham");
        expect(counts).toMatchObject({ total: 63, unverified: 2, setters: 4 });
        expect(detail.displayName).toBe("Phạm Ngọc Lan");
    });

    it("để tên trống khi không gửi tên hiển thị", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post<AdminUserDetail>(USERS_PATH, {
                username: "khach.moi",
                email: "khach.moi@example.com",
                role: "LEARNER",
            }),
        ).resolves.toMatchObject({ displayName: null });
    });

    it.each([
        ["username", { ...NEW_USER, username: "Lan Pham" }],
        ["username", { ...NEW_USER, username: "ab" }],
        ["email", { ...NEW_USER, email: "khong-hop-le" }],
        ["role", { ...NEW_USER, role: "BOSS" }],
        ["displayName", { ...NEW_USER, displayName: "a".repeat(61) }],
    ])("trả 400 kèm lỗi ở trường %s", async (field, body) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(USERS_PATH, body)).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });

    it("trả 409 khi username đã có người dùng", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post(USERS_PATH, { ...NEW_USER, username: "viet.luu" }),
        ).rejects.toMatchObject({ status: 409, code: "USERNAME_TAKEN" });
    });

    it("trả 409 khi email đã được dùng, không phân biệt hoa thường", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post(USERS_PATH, { ...NEW_USER, email: "Viet.Luu@Example.com" }),
        ).rejects.toMatchObject({ status: 409, code: "EMAIL_TAKEN" });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.post(USERS_PATH, NEW_USER)).rejects.toMatchObject({
            status: 403,
            code: "ACCESS_DENIED",
        });
    });
});
