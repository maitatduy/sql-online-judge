import { describe, expect, it } from "vitest";

import type { ExportUsersResponse } from "@/features/admin/users/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const EXPORT_PATH = "/api/admin/user-exports";

describe("POST /api/admin/user-exports", () => {
    it.each([
        { scope: "FILTERED", filters: { role: "SETTER" }, includeEmail: false },
        {
            scope: "SELECTED",
            usernames: ["viet.luu", "an.bui"],
            includeEmail: true,
            reason: "Khảo sát",
        },
        { scope: "ALL", includeEmail: false },
    ])("nhận yêu cầu xuất với %j", async (body) => {
        writeMockSession(ADMIN_SESSION_USER);

        const response = await apiClient.post<ExportUsersResponse>(EXPORT_PATH, body);

        expect(response.exportId).toMatch(/^export-/);
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

        await expect(apiClient.post(EXPORT_PATH, body)).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });

    it("trả 403 khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        await expect(
            apiClient.post(EXPORT_PATH, { scope: "ALL", includeEmail: false }),
        ).rejects.toMatchObject({ status: 403, code: "ACCESS_DENIED" });
    });
});
