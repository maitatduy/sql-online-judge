import { describe, expect, it } from "vitest";

import type {
    AdminUserDetail,
    AdminUsersResponse,
    UserCountsResponse,
} from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const DELETE_PATH = "/api/admin/users/tam.nguyen";

describe("DELETE /api/admin/users/{username}", () => {
    it("xoá mềm, ẩn khỏi danh sách, số liệu và chi tiết", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.delete(DELETE_PATH, { reason: "Tài khoản tạo nhầm" }),
        ).resolves.toBeUndefined();

        const list = await apiClient.get<AdminUsersResponse>("/api/admin/users", {
            params: { q: "tam.nguyen" },
        });
        const counts = await apiClient.get<UserCountsResponse>("/api/admin/users/summary");

        expect(list.items).toEqual([]);
        expect(counts).toMatchObject({ total: 61, unverified: 0 });
        await expect(apiClient.get<AdminUserDetail>(DELETE_PATH)).resolves.toMatchObject({
            username: "tam.nguyen",
            deletedAt: expect.stringMatching(/^\d{4}-\d{2}-\d{2}T/),
        });
    });

    it("chỉ liệt kê tài khoản đã xoá khi lọc deleted=true", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete(DELETE_PATH, { reason: "Tạo nhầm" });

        const deleted = await apiClient.get<AdminUsersResponse>("/api/admin/users", {
            params: { deleted: true },
        });

        expect(deleted.items.map((user) => user.username)).toEqual(["tam.nguyen"]);
        expect(deleted.totalItems).toBe(1);
    });

    it("không cho thao tác khác trên tài khoản đã xoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete(DELETE_PATH, { reason: "Tạo nhầm" });

        await expect(apiClient.patch(DELETE_PATH, { role: "SETTER" })).rejects.toMatchObject({
            status: 404,
        });
    });

    it("trả 404 khi xoá lại tài khoản đã xoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete(DELETE_PATH, { reason: "Lần đầu" });

        await expect(apiClient.delete(DELETE_PATH, { reason: "Lần hai" })).rejects.toMatchObject({
            status: 404,
            code: "USER_NOT_FOUND",
        });
    });

    it("trả 400 kèm lỗi ở trường reason khi thiếu lý do", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.delete(DELETE_PATH, { reason: " " })).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field: "reason" }],
        });
    });

    it("trả 422 khi quản trị viên tự xoá mình", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.delete("/api/admin/users/admin.huy", { reason: "Thử" }),
        ).rejects.toMatchObject({ status: 422, code: "CANNOT_MODIFY_SELF" });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.delete(DELETE_PATH, { reason: "Thử" })).rejects.toMatchObject({
            status: 403,
            code: "ACCESS_DENIED",
        });
    });
});

describe("POST /api/admin/users/{username}/restore", () => {
    it("khôi phục tài khoản đã xoá về danh sách", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete(DELETE_PATH, { reason: "Tạo nhầm" });

        await expect(
            apiClient.post<AdminUserDetail>(`${DELETE_PATH}/restore`),
        ).resolves.toMatchObject({ username: "tam.nguyen", deletedAt: null });

        const counts = await apiClient.get<UserCountsResponse>("/api/admin/users/summary");

        expect(counts.total).toBe(62);
    });

    it("trả 409 khi tài khoản chưa bị xoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post(`${DELETE_PATH}/restore`)).rejects.toMatchObject({
            status: 409,
            code: "USER_NOT_DELETED",
        });
    });

    it("trả 404 khi không có tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.post("/api/admin/users/khong.co/restore")).rejects.toMatchObject({
            status: 404,
            code: "USER_NOT_FOUND",
        });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.post(`${DELETE_PATH}/restore`)).rejects.toMatchObject({
            status: 403,
        });
    });
});
