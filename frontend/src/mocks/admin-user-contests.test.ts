import { describe, expect, it } from "vitest";

import type { ContestHistoryResponse } from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const historyPath = (username: string) => `/api/admin/users/${username}/contest-history`;

describe("GET /api/admin/users/{username}/contest-history", () => {
    it("trả các contest mới nhất trước, rating đang tạm giữ là null", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const history = await apiClient.get<ContestHistoryResponse>(historyPath("viet.luu"));

        expect(history).toMatchObject({ page: 1, size: 10, totalItems: 9, totalPages: 1 });
        expect(history.items.slice(0, 3)).toMatchObject([
            { contestTitle: "SQL Weekly #37", rank: 212, solvedCount: 2, ratingDelta: null },
            { contestTitle: "SQL Weekly #36", rank: 604, solvedCount: 1, ratingDelta: -18 },
            { contestTitle: "Window Function Cup", rank: 488, solvedCount: 2, ratingDelta: 9 },
        ]);
    });

    it("phân trang theo page và size", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const history = await apiClient.get<ContestHistoryResponse>(historyPath("quang.le"), {
            params: { page: 2, size: 10 },
        });

        expect(history).toMatchObject({ page: 2, totalItems: 14, totalPages: 2 });
        expect(history.items).toHaveLength(4);
    });

    it("trả danh sách rỗng khi chưa thi contest nào", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.get<ContestHistoryResponse>(historyPath("tam.nguyen")),
        ).resolves.toMatchObject({ items: [], totalItems: 0 });
    });

    it("vẫn đọc được lịch sử của tài khoản đã xoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete("/api/admin/users/viet.luu", { reason: "Tạo nhầm" });

        await expect(
            apiClient.get<ContestHistoryResponse>(historyPath("viet.luu")),
        ).resolves.toMatchObject({ totalItems: 9 });
    });

    it("trả 404 khi không có tài khoản và 403 khi không phải quản trị viên", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.get(historyPath("khong.co"))).rejects.toMatchObject({
            status: 404,
        });

        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.get(historyPath("viet.luu"))).rejects.toMatchObject({
            status: 403,
        });
    });
});
