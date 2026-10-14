import { describe, expect, it } from "vitest";

import type { UserNote, UserNotesResponse } from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const notesPath = (username: string) => `/api/admin/users/${username}/notes`;

describe("ghi chú nội bộ", () => {
    it("liệt kê ghi chú đã có", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { items } = await apiClient.get<UserNotesResponse>(notesPath("viet.luu"));

        expect(items).toMatchObject([
            {
                authorUsername: "mod.thu",
                body: "Đã nhắc nhở qua tin nhắn, người dùng hứa không tái phạm.",
            },
        ]);
    });

    it("thêm ghi chú với người viết là quản trị viên đang đăng nhập, mới nhất đứng đầu", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const note = await apiClient.post<UserNote>(notesPath("viet.luu"), {
            body: "  Theo dõi thêm contest Weekly #38  ",
        });
        const { items } = await apiClient.get<UserNotesResponse>(notesPath("viet.luu"));

        expect(note).toMatchObject({
            authorUsername: "admin.huy",
            body: "Theo dõi thêm contest Weekly #38",
        });
        expect(items.map((item) => item.id)).toEqual([note.id, items[1].id]);
    });

    it.each([{ body: "   " }, { body: "a".repeat(1001) }, {}])(
        "trả 400 kèm lỗi ở trường body khi nhận %j",
        async (body) => {
            writeMockSession(ADMIN_SESSION_USER);

            await expect(apiClient.post(notesPath("viet.luu"), body)).rejects.toMatchObject({
                status: 400,
                errors: [{ field: "body" }],
            });
        },
    );

    it("chỉ cho xem ghi chú của tài khoản đã xoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete("/api/admin/users/viet.luu", { reason: "Tạo nhầm" });

        await expect(apiClient.get(notesPath("viet.luu"))).resolves.toBeTruthy();
        await expect(apiClient.post(notesPath("viet.luu"), { body: "Thử" })).rejects.toMatchObject({
            status: 404,
        });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.get(notesPath("viet.luu"))).rejects.toMatchObject({ status: 403 });
    });
});
