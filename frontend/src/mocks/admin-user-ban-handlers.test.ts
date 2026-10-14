import { describe, expect, it } from "vitest";

import type { AdminUserDetail, UserCountsResponse } from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const DAY_MS = 24 * 60 * 60_000;
const BAN_PATH = "/api/admin/users/viet.luu/ban";
const UNBAN_PATH = "/api/admin/users/copy.paste/unban";
const VALID_BAN = { duration: "DAYS_7", reason: "Spam bình luận", notifyByEmail: true };

function daysFromNow(isoTime: string | null): number {
    return (new Date(isoTime ?? 0).getTime() - Date.now()) / DAY_MS;
}

describe("POST /api/admin/users/{username}/ban", () => {
    it.each([
        ["DAYS_7", 7],
        ["DAYS_30", 30],
    ])("khoá %s thì hết hạn sau %d ngày và ghi thêm một vi phạm", async (duration, days) => {
        writeMockSession(ADMIN_SESSION_USER);

        const user = await apiClient.post<AdminUserDetail>(BAN_PATH, { ...VALID_BAN, duration });

        expect(user.status).toBe("BANNED");
        expect(daysFromNow(user.bannedUntil)).toBeCloseTo(days, 1);
        expect(user.violations).toHaveLength(3);
        expect(user.violations[2]).toMatchObject({
            severity: "SEVERE",
            title: expect.stringMatching(/^Bị khoá đến \d{2}\/\d{2}\/\d{4}$/),
            detail: "Spam bình luận",
            status: "RESOLVED",
            handledBy: "admin.huy",
            handledAt: expect.stringMatching(/^\d{4}-\d{2}-\d{2}T/),
        });
    });

    it("khoá vĩnh viễn thì không có ngày hết hạn", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const user = await apiClient.post<AdminUserDetail>(BAN_PATH, {
            ...VALID_BAN,
            duration: "PERMANENT",
        });

        expect(user).toMatchObject({ status: "BANNED", bannedUntil: null });
        expect(user.violations[2].title).toBe("Bị khoá vĩnh viễn");
    });

    it("giữ trạng thái khoá cho các lần đọc sau và tăng số tài khoản bị khoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.post(BAN_PATH, VALID_BAN);

        const user = await apiClient.get<AdminUserDetail>("/api/admin/users/viet.luu");
        const counts = await apiClient.get<UserCountsResponse>("/api/admin/user-summary");

        expect(user.status).toBe("BANNED");
        expect(user.violations).toHaveLength(3);
        expect(counts.banned).toBe(3);
    });

    it.each([
        ["reason", { ...VALID_BAN, reason: "   " }],
        ["reason", { ...VALID_BAN, reason: "a".repeat(501) }],
        ["duration", { ...VALID_BAN, duration: "DAYS_3" }],
        ["notifyByEmail", { ...VALID_BAN, notifyByEmail: "yes" }],
    ])("trả 400 kèm lỗi ở trường %s", async (field, body) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(BAN_PATH, body)).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });

    it("trả 404 khi không có tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/users/khong.co/ban", VALID_BAN),
        ).rejects.toMatchObject({ status: 404, code: "USER_NOT_FOUND" });
    });

    it("trả 409 khi tài khoản đã bị khoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/users/spam.bot88/ban", VALID_BAN),
        ).rejects.toMatchObject({ status: 409, code: "USER_ALREADY_BANNED" });
    });

    it("trả 422 khi quản trị viên tự khoá mình", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/users/admin.huy/ban", VALID_BAN),
        ).rejects.toMatchObject({ status: 422, code: "CANNOT_MODIFY_SELF" });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.post(BAN_PATH, VALID_BAN)).rejects.toMatchObject({
            status: 403,
            code: "ACCESS_DENIED",
        });
    });
});

describe("POST /api/admin/users/{username}/unban", () => {
    it("mở khoá, xoá ngày hết hạn và giữ lịch sử vi phạm", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const user = await apiClient.post<AdminUserDetail>(UNBAN_PATH, { reason: "Hết hạn" });

        expect(user).toMatchObject({ status: "ACTIVE", bannedUntil: null });
        expect(user.violations).toHaveLength(1);
    });

    it("mở khoá được tài khoản vừa bị khoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.post(BAN_PATH, VALID_BAN);

        await expect(
            apiClient.post<AdminUserDetail>("/api/admin/users/viet.luu/unban", { reason: "Nhầm" }),
        ).resolves.toMatchObject({ status: "ACTIVE", bannedUntil: null });
    });

    it("trả 400 kèm lỗi ở trường reason khi thiếu lý do", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(UNBAN_PATH, { reason: "" })).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field: "reason" }],
        });
    });

    it("trả 409 khi tài khoản không bị khoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/users/viet.luu/unban", { reason: "Thử" }),
        ).rejects.toMatchObject({ status: 409, code: "USER_NOT_BANNED" });
    });

    it("trả 404 khi không có tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.post("/api/admin/users/khong.co/unban", { reason: "Thử" }),
        ).rejects.toMatchObject({ status: 404, code: "USER_NOT_FOUND" });
    });
});
