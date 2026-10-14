import { describe, expect, it } from "vitest";

import { accountStateOf } from "./account-status";

describe("accountStateOf", () => {
    it("ưu tiên trạng thái đã xoá hơn mọi trạng thái khác", () => {
        expect(
            accountStateOf({
                status: "BANNED",
                isEmailVerified: false,
                deletedAt: "2026-10-14T03:00:00Z",
            }),
        ).toBe("DELETED");
    });

    it.each([
        ["ACTIVE", true, "ACTIVE"],
        ["ACTIVE", false, "UNVERIFIED"],
        ["BANNED", true, "BANNED"],
        ["BANNED", false, "BANNED"],
    ] as const)(
        "status %s, đã xác minh %s thì hiển thị %s",
        (status, isEmailVerified, expected) => {
            expect(accountStateOf({ status, isEmailVerified, deletedAt: null })).toBe(expected);
        },
    );
});
