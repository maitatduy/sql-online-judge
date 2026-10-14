import { describe, expect, it } from "vitest";

import type { ProfileResponse } from "@/features/profile/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, writeMockSession } from "./session";

describe("hồ sơ công khai của tài khoản trong trang quản trị", () => {
    it("trả hồ sơ dựng từ dữ liệu tài khoản", async () => {
        const profile = await apiClient.get<ProfileResponse>("/api/users/viet.luu");

        expect(profile).toMatchObject({
            username: "viet.luu",
            displayName: "Lưu Quốc Việt",
            rating: 1212,
            followerCount: 0,
            badges: [],
        });
        expect(profile.stats.solvedCount).toBe(41);
        expect(
            Object.values(profile.stats.solvedByDifficulty).reduce(
                (sum, difficulty) => sum + difficulty.solved,
                0,
            ),
        ).toBe(41);
    });

    it("dùng username làm tên hiển thị và ghi chưa xếp hạng khi chưa có rating", async () => {
        await expect(
            apiClient.get<ProfileResponse>("/api/users/spam.bot88"),
        ).resolves.toMatchObject({
            displayName: "spam.bot88",
            rating: 0,
            rankTitle: "Chưa xếp hạng",
        });
    });

    it("trả danh sách rỗng cho bài nộp, lịch sử rating và hoạt động", async () => {
        await expect(
            apiClient.get("/api/users/viet.luu/submissions", { params: { size: 5 } }),
        ).resolves.toEqual({ items: [], nextCursor: null });
        await expect(apiClient.get("/api/users/viet.luu/rating-history")).resolves.toEqual({
            items: [],
        });
    });

    it("giữ nguyên hồ sơ đã khai báo sẵn", async () => {
        await expect(apiClient.get<ProfileResponse>("/api/users/minh.anh")).resolves.toMatchObject({
            school: "ĐH Bách khoa Hà Nội",
            followerCount: 128,
        });
    });

    it("trả 404 cho tài khoản đã xoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete("/api/admin/users/viet.luu", { reason: "Tạo nhầm" });

        await expect(apiClient.get("/api/users/viet.luu")).rejects.toMatchObject({
            status: 404,
            code: "USER_NOT_FOUND",
        });
    });
});
