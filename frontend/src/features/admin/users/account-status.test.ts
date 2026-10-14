import { describe, expect, it } from "vitest";

import { accountStateOf } from "./account-status";

describe("accountStateOf", () => {
    it.each([
        ["ACTIVE", true, "ACTIVE"],
        ["ACTIVE", false, "UNVERIFIED"],
        ["BANNED", true, "BANNED"],
        ["BANNED", false, "BANNED"],
    ] as const)(
        "status %s, đã xác minh %s thì hiển thị %s",
        (status, isEmailVerified, expected) => {
            expect(accountStateOf({ status, isEmailVerified })).toBe(expected);
        },
    );
});
