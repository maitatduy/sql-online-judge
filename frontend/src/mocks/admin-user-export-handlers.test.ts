import { describe, expect, it } from "vitest";

import type { AdminUsersResponse } from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const EXPORT_PATH = "/api/admin/user-exports";
const MAX_PAGE_SIZE = 100;

async function csvLines(blob: Blob): Promise<string[]> {
    return (await blob.text()).trim().split("\n");
}

function usernameOf(csvLine: string): string {
    return csvLine.split(",")[0];
}

describe("POST /api/admin/user-exports", () => {
    it.each([
        { scope: "FILTERED", filters: { role: "SETTER" }, includeEmail: false },
        { scope: "ALL", includeEmail: false },
    ])("trả file CSV tải trực tiếp với %j", async (body) => {
        writeMockSession(ADMIN_SESSION_USER);

        const file = await apiClient.postForFile(EXPORT_PATH, body);

        expect(file.fileName).toMatch(/^users-\d{8}-\d{4}\.csv$/);
        expect(file.blob.type).toMatch(/^text\/csv;\s?charset=utf-8$/);
        expect((await csvLines(file.blob))[0]).toBe("username,role,status,joinedAt");
    });

    it.each([
        { role: "SETTER" },
        { q: "viet" },
        { status: "BANNED" },
        { q: "learner", role: "LEARNER", status: "ACTIVE" },
    ])("xuất theo bộ lọc %j đúng những tài khoản danh sách đang hiện", async (filters) => {
        writeMockSession(ADMIN_SESSION_USER);
        const listed = await apiClient.get<AdminUsersResponse>("/api/admin/users", {
            params: { ...filters, size: MAX_PAGE_SIZE },
        });

        const file = await apiClient.postForFile(EXPORT_PATH, {
            scope: "FILTERED",
            filters,
            includeEmail: false,
        });

        const exportedUsernames = (await csvLines(file.blob)).slice(1).map(usernameOf);
        expect(exportedUsernames.length).toBeGreaterThan(0);
        expect(exportedUsernames).toHaveLength(listed.totalItems);
        expect(exportedUsernames).toEqual(listed.items.map((user) => user.username));
    });

    it("chỉ xuất những người đã chọn và thêm cột email khi xuất kèm email", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const file = await apiClient.postForFile(EXPORT_PATH, {
            scope: "SELECTED",
            usernames: ["viet.luu", "an.bui"],
            includeEmail: true,
            reason: "Khảo sát",
        });

        const [header, ...rows] = await csvLines(file.blob);
        expect(header).toBe("username,email,role,status,joinedAt");
        expect(rows.map((row) => row.split(",").slice(0, 2))).toEqual([
            ["viet.luu", "viet.luu@example.com"],
            ["an.bui", "an.bui@example.com"],
        ]);
    });

    it.each([
        ["scope", { scope: "SOME", includeEmail: false }],
        ["usernames", { scope: "SELECTED", usernames: [], includeEmail: false }],
        ["filters", { scope: "FILTERED", includeEmail: false }],
        ["includeEmail", { scope: "ALL", includeEmail: "yes" }],
        ["reason", { scope: "ALL", includeEmail: true }],
        ["reason", { scope: "ALL", includeEmail: true, reason: "  " }],
    ])("trả 400 kèm lỗi ở trường %s", async (field, body) => {
        writeMockSession(ADMIN_SESSION_USER);

        await expect(apiClient.postForFile(EXPORT_PATH, body)).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(
            apiClient.postForFile(EXPORT_PATH, { scope: "ALL", includeEmail: false }),
        ).rejects.toMatchObject({ status: 403, code: "ACCESS_DENIED" });
    });
});
