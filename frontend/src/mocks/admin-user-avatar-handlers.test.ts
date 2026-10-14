import { describe, expect, it } from "vitest";

import type {
    AdminUserDetail,
    AdminUsersResponse,
    UserAvatarResponse,
} from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const AVATAR_PATH = "/api/admin/users/viet.luu/avatar";
const TWO_MB = 2 * 1024 * 1024;

function avatarForm(file: File) {
    const form = new FormData();
    form.append("file", file);
    return form;
}

const pngFile = () => new File(["png"], "avatar.png", { type: "image/png" });

describe("PUT /api/admin/users/{username}/avatar", () => {
    it("lưu ảnh và trả đường dẫn ảnh", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const response = await apiClient.putForm<UserAvatarResponse>(
            AVATAR_PATH,
            avatarForm(pngFile()),
        );

        expect(response.avatarUrl).toBe("data:image/png;base64,cG5n");
    });

    it("hiện ảnh ở chi tiết và danh sách", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.putForm(AVATAR_PATH, avatarForm(pngFile()));

        const detail = await apiClient.get<AdminUserDetail>("/api/admin/users/viet.luu");
        const list = await apiClient.get<AdminUsersResponse>("/api/admin/users", {
            params: { q: "viet.luu" },
        });

        expect(detail.avatarUrl).toBe("data:image/png;base64,cG5n");
        expect(list.items[0].avatarUrl).toBe("data:image/png;base64,cG5n");
    });

    it.each([
        ["gif", new File(["gif"], "a.gif", { type: "image/gif" })],
        ["quá 2 MB", new File([new Uint8Array(TWO_MB + 1)], "a.png", { type: "image/png" })],
    ])("trả 400 kèm lỗi ở trường file khi ảnh %s", async (_case, file) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.putForm(AVATAR_PATH, avatarForm(file))).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field: "file" }],
        });
    });

    it("trả 400 khi không gửi tệp", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.putForm(AVATAR_PATH, new FormData())).rejects.toMatchObject({
            status: 400,
            errors: [{ field: "file" }],
        });
    });

    it("trả 404 khi không có tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.putForm("/api/admin/users/khong.co/avatar", avatarForm(pngFile())),
        ).rejects.toMatchObject({ status: 404, code: "USER_NOT_FOUND" });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.putForm(AVATAR_PATH, avatarForm(pngFile()))).rejects.toMatchObject({
            status: 403,
            code: "ACCESS_DENIED",
        });
    });
});

describe("DELETE /api/admin/users/{username}/avatar", () => {
    it("xoá ảnh đã có", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.putForm(AVATAR_PATH, avatarForm(pngFile()));

        await expect(apiClient.delete(AVATAR_PATH)).resolves.toBeUndefined();
        await expect(
            apiClient.get<AdminUserDetail>("/api/admin/users/viet.luu"),
        ).resolves.toMatchObject({ avatarUrl: null });
    });

    it("trả 404 khi không có tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.delete("/api/admin/users/khong.co/avatar")).rejects.toMatchObject({
            status: 404,
        });
    });
});
