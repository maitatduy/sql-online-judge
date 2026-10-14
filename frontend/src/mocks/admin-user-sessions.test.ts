import { describe, expect, it } from "vitest";

import type { UserSessionsResponse } from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const sessionsPath = (username: string) => `/api/admin/users/${username}/sessions`;

async function sessionsOf(username: string) {
    return (await apiClient.get<UserSessionsResponse>(sessionsPath(username))).items;
}

describe("phiên đăng nhập của tài khoản", () => {
    it("liệt kê phiên mới hoạt động nhất trước và đánh dấu vị trí lạ", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const sessions = await sessionsOf("viet.luu");

        expect(sessions.map(({ browser, os, city }) => `${browser} · ${os} · ${city}`)).toEqual([
            "Chrome · Windows · Hà Nội",
            "Safari · iPhone · Hà Nội",
            "Chrome · Windows · TP.HCM",
        ]);
        expect(sessions.map((session) => session.isActiveNow)).toEqual([true, false, false]);
        expect(sessions.map((session) => session.isUnusualLocation)).toEqual([false, false, true]);
        expect(Object.keys(sessions[0]).sort()).toEqual([
            "browser",
            "city",
            "id",
            "isActiveNow",
            "isUnusualLocation",
            "lastActiveAt",
            "os",
        ]);
    });

    it("đăng xuất một phiên", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const [, phone] = await sessionsOf("viet.luu");

        await expect(
            apiClient.delete(`${sessionsPath("viet.luu")}/${phone.id}`),
        ).resolves.toBeUndefined();

        expect((await sessionsOf("viet.luu")).map((session) => session.id)).not.toContain(phone.id);
    });

    it("đăng xuất mọi thiết bị", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await apiClient.delete(sessionsPath("viet.luu"));

        expect(await sessionsOf("viet.luu")).toEqual([]);
    });

    it("trả 404 khi phiên không tồn tại", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(
            apiClient.delete(`${sessionsPath("viet.luu")}/khong-co`),
        ).rejects.toMatchObject({ status: 404, code: "SESSION_NOT_FOUND" });
    });

    it("chỉ cho xem phiên của tài khoản đã xoá, không cho đăng xuất", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete("/api/admin/users/viet.luu", { reason: "Tạo nhầm" });

        expect(await sessionsOf("viet.luu")).toHaveLength(3);
        await expect(apiClient.delete(sessionsPath("viet.luu"))).rejects.toMatchObject({
            status: 404,
        });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(apiClient.get(sessionsPath("viet.luu"))).rejects.toMatchObject({
            status: 403,
        });
    });
});
