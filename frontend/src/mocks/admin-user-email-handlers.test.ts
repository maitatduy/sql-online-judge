import { describe, expect, it } from "vitest";

import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const RESET_PATH = "/api/admin/users/viet.luu/password-reset-email";
const VERIFICATION_PATH = "/api/admin/users/tam.nguyen/verification-email";

describe("POST /api/admin/users/{username}/password-reset-email", () => {
    it("nhận yêu cầu gửi email đặt lại mật khẩu", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(RESET_PATH)).resolves.toEqual({ username: "viet.luu" });
    });

    it("trả 404 khi không có tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/users/khong.co/password-reset-email"),
        ).rejects.toMatchObject({ status: 404, code: "USER_NOT_FOUND" });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.post(RESET_PATH)).rejects.toMatchObject({
            status: 403,
            code: "ACCESS_DENIED",
        });
    });
});

describe("POST /api/admin/users/{username}/verification-email", () => {
    it("nhận yêu cầu gửi lại email xác thực", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(VERIFICATION_PATH)).resolves.toEqual({
            username: "tam.nguyen",
        });
    });

    it("trả 409 khi email đã được xác minh", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/users/viet.luu/verification-email"),
        ).rejects.toMatchObject({ status: 409, code: "EMAIL_ALREADY_VERIFIED" });
    });

    it("trả 404 khi không có tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/users/khong.co/verification-email"),
        ).rejects.toMatchObject({ status: 404, code: "USER_NOT_FOUND" });
    });
});
