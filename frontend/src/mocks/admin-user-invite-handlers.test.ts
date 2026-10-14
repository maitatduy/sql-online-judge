import { describe, expect, it } from "vitest";

import type { InviteUsersResponse } from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const INVITE_PATH = "/api/admin/user-invitations";
const VALID_INVITE = { emails: ["a@example.com", "b@example.com"], role: "SETTER" };

describe("POST /api/admin/user-invitations", () => {
    it("gửi lời mời và trả số lời mời đã gửi", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post<InviteUsersResponse>(INVITE_PATH, {
                ...VALID_INVITE,
                message: "Chào mừng CLB",
            }),
        ).resolves.toEqual({ invitedCount: 2 });
    });

    it("đếm mỗi email một lần dù gửi trùng", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post<InviteUsersResponse>(INVITE_PATH, {
                emails: ["a@example.com", "A@example.com"],
                role: "MODERATOR",
            }),
        ).resolves.toEqual({ invitedCount: 1 });
    });

    it.each([
        ["emails", { ...VALID_INVITE, emails: [] }],
        ["emails", { ...VALID_INVITE, emails: ["a@example.com", "khong-hop-le"] }],
        ["emails", { ...VALID_INVITE, emails: Array.from({ length: 51 }, (_, i) => `u${i}@x.vn`) }],
        ["role", { ...VALID_INVITE, role: "ADMIN" }],
        ["message", { ...VALID_INVITE, message: "a".repeat(301) }],
    ])("trả 400 kèm lỗi ở trường %s", async (field, body) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(INVITE_PATH, body)).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.post(INVITE_PATH, VALID_INVITE)).rejects.toMatchObject({
            status: 403,
            code: "ACCESS_DENIED",
        });
    });
});
