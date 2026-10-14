import { describe, expect, it } from "vitest";

import type {
    AdminUserDetail,
    AdminUsersResponse,
    UserCountsResponse,
} from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const USERS_PATH = "/api/admin/users";

describe("GET /api/admin/users", () => {
    it("trả trang đầu 20 tài khoản, mới tham gia đứng trước", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const response = await apiClient.get<AdminUsersResponse>(USERS_PATH);

        expect(response).toMatchObject({ page: 1, size: 20, totalItems: 62, totalPages: 4 });
        expect(response.items).toHaveLength(20);
        expect(response.items.slice(0, 3).map((user) => user.username)).toEqual([
            "tam.nguyen",
            "spam.bot88",
            "datahust",
        ]);
    });

    it("trả phần còn lại ở trang cuối", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const response = await apiClient.get<AdminUsersResponse>(USERS_PATH, {
            params: { page: 4 },
        });

        expect(response.page).toBe(4);
        expect(response.items).toHaveLength(2);
    });

    it("trả danh sách rỗng khi trang vượt quá tổng số trang", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const response = await apiClient.get<AdminUsersResponse>(USERS_PATH, {
            params: { page: 9 },
        });

        expect(response).toMatchObject({ items: [], totalItems: 62, totalPages: 4 });
    });

    it("chỉ trả các trường của dòng danh sách", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { items } = await apiClient.get<AdminUsersResponse>(USERS_PATH);

        expect(Object.keys(items[0]).sort()).toEqual([
            "acceptedCount",
            "avatarUrl",
            "bannedUntil",
            "deletedAt",
            "email",
            "id",
            "isEmailVerified",
            "isOnline",
            "joinedAt",
            "lastActiveAt",
            "rating",
            "role",
            "status",
            "username",
        ]);
    });

    it.each([
        ["page", { page: 0 }],
        ["size", { size: 101 }],
        ["size", { size: 0 }],
    ])("trả 400 kèm lỗi ở trường %s khi tham số ngoài khoảng", async (field, params) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.get(USERS_PATH, { params })).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.get(USERS_PATH)).rejects.toMatchObject({
            status: 403,
            code: "ACCESS_DENIED",
        });
    });
});

describe("GET /api/admin/users - lọc", () => {
    async function usernamesFor(params: Record<string, string | boolean>) {
        writeMockSession(ADMIN_SESSION_USER);
        const response = await apiClient.get<AdminUsersResponse>(USERS_PATH, { params });
        return response.items.map((user) => user.username);
    }

    it("tìm theo một phần username, không phân biệt hoa thường", async () => {
        expect(await usernamesFor({ q: "VIET" })).toEqual(["viet.luu"]);
    });

    it("tìm theo một phần email", async () => {
        expect(await usernamesFor({ q: "@sqljudge.dev" })).toEqual(["mod.thu", "admin.huy"]);
    });

    it("lọc theo vai trò", async () => {
        expect(await usernamesFor({ role: "SETTER" })).toEqual(["datahust", "an.bui", "quang.le"]);
    });

    it("lọc tài khoản bị khoá", async () => {
        expect(await usernamesFor({ status: "BANNED" })).toEqual(["spam.bot88", "copy.paste"]);
    });

    it("lọc tài khoản chưa xác minh email", async () => {
        expect(await usernamesFor({ status: "ACTIVE", emailVerified: false })).toEqual([
            "tam.nguyen",
        ]);
    });

    it("kết hợp nhiều điều kiện và tính lại tổng số", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const response = await apiClient.get<AdminUsersResponse>(USERS_PATH, {
            params: { q: "learner", role: "LEARNER", status: "ACTIVE", emailVerified: true },
        });

        expect(response).toMatchObject({ totalItems: 50, totalPages: 3 });
    });

    it.each([
        ["role", { role: "BOSS" }],
        ["status", { status: "DELETED" }],
        ["emailVerified", { emailVerified: "maybe" }],
    ])("trả 400 kèm lỗi ở trường %s khi giá trị lạ", async (field, params) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.get(USERS_PATH, { params })).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });
});

describe("GET /api/admin/user-summary", () => {
    const SUMMARY_PATH = "/api/admin/user-summary";

    it("trả số tài khoản theo từng nhóm", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.get<UserCountsResponse>(SUMMARY_PATH)).resolves.toEqual({
            total: 62,
            unverified: 1,
            setters: 3,
            moderators: 2,
            banned: 2,
        });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.get(SUMMARY_PATH)).rejects.toMatchObject({
            status: 403,
            code: "ACCESS_DENIED",
        });
    });
});

describe("GET /api/admin/users/{username}", () => {
    it("trả chi tiết kèm tên, kênh đăng ký và lịch sử vi phạm", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const user = await apiClient.get<AdminUserDetail>("/api/admin/users/viet.luu");

        expect(user).toMatchObject({
            username: "viet.luu",
            email: "viet.luu@example.com",
            displayName: "Lưu Quốc Việt",
            signupProvider: "GITHUB",
            status: "ACTIVE",
        });
        expect(user.violations).toEqual([
            {
                id: "viet.luu-1",
                severity: "SEVERE",
                title: "Nghi đạo code trong contest",
                detail: "Weekly #37, đang chờ xử lý",
            },
            {
                id: "viet.luu-2",
                severity: "MINOR",
                title: "Spam bình luận",
                detail: "12/09, mod.thu đã cảnh cáo",
            },
        ]);
    });

    it("trả tên rỗng và không vi phạm cho tài khoản chưa khai báo gì", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.get<AdminUserDetail>("/api/admin/users/learner.07"),
        ).resolves.toMatchObject({ displayName: null, signupProvider: "EMAIL", violations: [] });
    });

    it("trả 404 khi không có tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.get("/api/admin/users/khong.co")).rejects.toMatchObject({
            status: 404,
            code: "USER_NOT_FOUND",
        });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.get("/api/admin/users/viet.luu")).rejects.toMatchObject({
            status: 403,
            code: "ACCESS_DENIED",
        });
    });
});

describe("PATCH /api/admin/users/{username}", () => {
    const USER_PATH = "/api/admin/users/viet.luu";

    it("đổi vai trò và trả chi tiết mới", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.patch<AdminUserDetail>(USER_PATH, { role: "SETTER", reason: "Ra đề" }),
        ).resolves.toMatchObject({ username: "viet.luu", role: "SETTER" });
    });

    it("phản ánh vai trò mới ở chi tiết, danh sách và số liệu", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.patch(USER_PATH, { role: "SETTER", reason: "Ra đề" });

        const detail = await apiClient.get<AdminUserDetail>(USER_PATH);
        const setters = await apiClient.get<AdminUsersResponse>(USERS_PATH, {
            params: { role: "SETTER" },
        });
        const counts = await apiClient.get<UserCountsResponse>("/api/admin/user-summary");

        expect(detail.role).toBe("SETTER");
        expect(setters.items.map((user) => user.username)).toContain("viet.luu");
        expect(counts.setters).toBe(4);
    });

    it("trả 400 kèm lỗi ở trường reason khi đổi vai trò mà không có lý do", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.patch(USER_PATH, { role: "SETTER" })).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field: "reason" }],
        });
    });

    it("đổi tên hiển thị mà không đổi vai trò", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await apiClient.patch(USER_PATH, { displayName: "  Việt Lưu  " });

        await expect(apiClient.get<AdminUserDetail>(USER_PATH)).resolves.toMatchObject({
            displayName: "Việt Lưu",
            role: "LEARNER",
        });
    });

    it("xoá tên hiển thị khi gửi null", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.patch<AdminUserDetail>(USER_PATH, { displayName: null }),
        ).resolves.toMatchObject({ displayName: null });
    });

    it("cho quản trị viên tự đổi tên hiển thị của mình", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.patch<AdminUserDetail>("/api/admin/users/admin.huy", {
                displayName: "Huy Trần",
                role: "ADMIN",
            }),
        ).resolves.toMatchObject({ displayName: "Huy Trần", role: "ADMIN" });
    });

    it.each([
        ["displayName", { displayName: "a".repeat(61) }],
        ["displayName", { displayName: "   " }],
        ["displayName", { displayName: 42 }],
    ])("trả 400 kèm lỗi ở trường %s", async (field, body) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.patch(USER_PATH, body)).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });

    it.each([{ role: "BOSS" }, { role: null }, {}])(
        "trả 400 kèm lỗi ở trường role khi nhận %j",
        async (body) => {
            writeMockSession(ADMIN_SESSION_USER);

            await expect(apiClient.patch(USER_PATH, body)).rejects.toMatchObject({
                status: 400,
                code: "VALIDATION_FAILED",
                errors: [{ field: "role" }],
            });
        },
    );

    it("trả 404 khi không có tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.patch("/api/admin/users/khong.co", { role: "SETTER", reason: "Thử" }),
        ).rejects.toMatchObject({ status: 404, code: "USER_NOT_FOUND" });
    });

    it("trả 422 khi quản trị viên tự đổi vai trò của mình", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.patch("/api/admin/users/admin.huy", { role: "LEARNER", reason: "Thử" }),
        ).rejects.toMatchObject({ status: 422, code: "CANNOT_MODIFY_SELF" });
    });

    it("trả 403 và không đổi gì khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(
            apiClient.patch(USER_PATH, { role: "ADMIN", reason: "Thử" }),
        ).rejects.toMatchObject({
            status: 403,
            code: "ACCESS_DENIED",
        });

        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.get<AdminUserDetail>(USER_PATH)).resolves.toMatchObject({
            role: "LEARNER",
        });
    });
});
