import { describe, expect, it } from "vitest";

import type { UserRestrictions } from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const restrictionsPath = (username: string) => `/api/admin/users/${username}/restrictions`;

describe("hạn chế của tài khoản", () => {
    it("mặc định 20 tin AI mỗi ngày và được thi contest có xếp hạng", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.get<UserRestrictions>(restrictionsPath("viet.luu")),
        ).resolves.toEqual({ aiDailyQuota: 20, isBannedFromRatedContests: false });
    });

    it("lưu hạn mức AI và lệnh cấm thi kèm lý do", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.patch<UserRestrictions>(restrictionsPath("viet.luu"), {
                aiDailyQuota: 0,
                isBannedFromRatedContests: true,
                reason: "Nghi gian lận",
            }),
        ).resolves.toEqual({ aiDailyQuota: 0, isBannedFromRatedContests: true });
        await expect(
            apiClient.get<UserRestrictions>(restrictionsPath("viet.luu")),
        ).resolves.toEqual({ aiDailyQuota: 0, isBannedFromRatedContests: true });
    });

    it.each([
        ["aiDailyQuota", { aiDailyQuota: 201, reason: "Thử" }],
        ["aiDailyQuota", { aiDailyQuota: -1, reason: "Thử" }],
        ["aiDailyQuota", { aiDailyQuota: 2.5, reason: "Thử" }],
        ["isBannedFromRatedContests", { isBannedFromRatedContests: "có", reason: "Thử" }],
        ["reason", { aiDailyQuota: 5 }],
        ["aiDailyQuota", { reason: "Không đổi gì" }],
    ])("trả 400 kèm lỗi ở trường %s", async (field, body) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.patch(restrictionsPath("viet.luu"), body)).rejects.toMatchObject({
            status: 400,
            errors: [{ field }],
        });
    });

    it("trả 422 khi tự đặt hạn chế cho chính mình", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.patch(restrictionsPath("admin.huy"), { aiDailyQuota: 5, reason: "Thử" }),
        ).rejects.toMatchObject({ status: 422, code: "CANNOT_MODIFY_SELF" });
    });

    it("chỉ cho xem hạn chế của tài khoản đã xoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete("/api/admin/users/viet.luu", { reason: "Tạo nhầm" });

        await expect(apiClient.get(restrictionsPath("viet.luu"))).resolves.toBeTruthy();
        await expect(
            apiClient.patch(restrictionsPath("viet.luu"), { aiDailyQuota: 5, reason: "Thử" }),
        ).rejects.toMatchObject({ status: 404 });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.get(restrictionsPath("viet.luu"))).rejects.toMatchObject({
            status: 403,
        });
    });
});
