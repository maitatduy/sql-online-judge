import { describe, expect, it } from "vitest";

import type { PublicProfile, RatingSummary, SolvedSummary } from "@/features/profile/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, writeMockSession } from "./session";

describe("hồ sơ công khai của tài khoản trong trang quản trị", () => {
    it("trả hồ sơ, rating và số bài đã giải dựng từ dữ liệu tài khoản", async () => {
        const profile = await apiClient.get<PublicProfile>("/api/users/viet.luu");
        const rating = await apiClient.get<RatingSummary>(`/api/leaderboard/${profile.userId}`);
        const solved = await apiClient.get<SolvedSummary>(
            `/api/solved-summaries/${profile.userId}`,
        );

        expect(profile).toMatchObject({ username: "viet.luu", displayName: "Lưu Quốc Việt" });
        expect(rating.rating).toBe(1212);
        expect(solved.solvedCount).toBe(41);
        expect(
            Object.values(solved.byDifficulty).reduce(
                (sum, difficulty) => sum + difficulty.solved,
                0,
            ),
        ).toBe(41);
    });

    it("dùng username làm tên hiển thị và không có rating khi chưa thi", async () => {
        const profile = await apiClient.get<PublicProfile>("/api/users/spam.bot88");
        const rating = await apiClient.get<RatingSummary>(`/api/leaderboard/${profile.userId}`);

        expect(profile.displayName).toBe("spam.bot88");
        expect(rating).toMatchObject({ rating: null, rank: null, contestsPlayed: 0 });
    });

    it("trả danh sách rỗng cho bài nộp, lịch sử rating và hoạt động", async () => {
        const { userId } = await apiClient.get<PublicProfile>("/api/users/viet.luu");

        await expect(
            apiClient.get(`/api/submissions/users/${userId}`, { params: { size: 5 } }),
        ).resolves.toEqual({ items: [], nextCursor: null });
        await expect(apiClient.get(`/api/leaderboard/${userId}/history`)).resolves.toEqual({
            items: [],
        });
        await expect(apiClient.get(`/api/submissions/users/${userId}/activity`)).resolves.toEqual({
            totalSubmissions: 0,
            days: [],
        });
    });

    it("giữ nguyên hồ sơ đã khai báo sẵn", async () => {
        await expect(apiClient.get<PublicProfile>("/api/users/minh.anh")).resolves.toMatchObject({
            school: "ĐH Bách khoa Hà Nội",
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
