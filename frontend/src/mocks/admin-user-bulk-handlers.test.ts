import { describe, expect, it } from "vitest";

import type {
    AdminUserDetail,
    BulkUserActionResponse,
    UserCountsResponse,
} from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const BULK_BAN_PATH = "/api/admin/users/bulk-ban";
const BULK_ROLE_PATH = "/api/admin/users/bulk-role";
const VALID_BAN = { duration: "DAYS_7", reason: "Spam hàng loạt", notifyByEmail: true };
const VALID_ROLE = { role: "SETTER", reason: "CLB ra đề" };

describe("POST /api/admin/users/bulk-ban", () => {
    it("khoá các tài khoản hợp lệ và trả danh sách bị bỏ qua", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const response = await apiClient.post<BulkUserActionResponse>(BULK_BAN_PATH, {
            ...VALID_BAN,
            usernames: ["tam.nguyen", "datahust", "spam.bot88", "admin.huy", "khong.co"],
        });

        expect(response).toEqual({
            affectedCount: 2,
            skippedUsernames: ["spam.bot88", "admin.huy", "khong.co"],
        });
    });

    it("lưu trạng thái khoá kèm vi phạm và tăng số tài khoản bị khoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.post(BULK_BAN_PATH, {
            ...VALID_BAN,
            duration: "PERMANENT",
            usernames: ["tam.nguyen", "datahust"],
        });

        const user = await apiClient.get<AdminUserDetail>("/api/admin/users/datahust");
        const counts = await apiClient.get<UserCountsResponse>("/api/admin/users/summary");

        expect(user).toMatchObject({ status: "BANNED", bannedUntil: null });
        expect(user.violations.at(-1)).toMatchObject({
            title: "Bị khoá vĩnh viễn",
            detail: "Spam hàng loạt",
        });
        expect(counts.banned).toBe(4);
    });

    it.each([
        ["usernames", { ...VALID_BAN, usernames: [] }],
        ["usernames", { ...VALID_BAN, usernames: Array.from({ length: 101 }, (_, i) => `u${i}`) }],
        ["usernames", { ...VALID_BAN, usernames: "tam.nguyen" }],
        ["reason", { ...VALID_BAN, reason: " ", usernames: ["tam.nguyen"] }],
        ["duration", { ...VALID_BAN, duration: "DAYS_1", usernames: ["tam.nguyen"] }],
    ])("trả 400 kèm lỗi ở trường %s", async (field, body) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(BULK_BAN_PATH, body)).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(
            apiClient.post(BULK_BAN_PATH, { ...VALID_BAN, usernames: ["tam.nguyen"] }),
        ).rejects.toMatchObject({ status: 403, code: "ACCESS_DENIED" });
    });
});

describe("POST /api/admin/users/bulk-role", () => {
    it("đổi vai trò các tài khoản hợp lệ và bỏ qua chính mình", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const response = await apiClient.post<BulkUserActionResponse>(BULK_ROLE_PATH, {
            ...VALID_ROLE,
            usernames: ["tam.nguyen", "viet.luu", "admin.huy"],
        });
        const counts = await apiClient.get<UserCountsResponse>("/api/admin/users/summary");

        expect(response).toEqual({ affectedCount: 2, skippedUsernames: ["admin.huy"] });
        expect(counts.setters).toBe(5);
    });

    it.each([
        ["role", { ...VALID_ROLE, role: "BOSS", usernames: ["tam.nguyen"] }],
        ["reason", { ...VALID_ROLE, reason: "", usernames: ["tam.nguyen"] }],
        ["usernames", { ...VALID_ROLE, usernames: [] }],
    ])("trả 400 kèm lỗi ở trường %s", async (field, body) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(BULK_ROLE_PATH, body)).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });
});
