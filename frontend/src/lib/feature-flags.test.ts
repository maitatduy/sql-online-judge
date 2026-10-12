import { describe, expect, it } from "vitest";

import { isOAuthEnabled } from "./feature-flags";

describe("isOAuthEnabled", () => {
    it.each([
        ["development", undefined, true],
        ["test", undefined, true],
        ["production", undefined, false],
        ["production", "false", false],
        ["production", "true", true],
    ])("NODE_ENV=%s, cờ=%s thì trả về %s", (nodeEnv, oauthFlag, expected) => {
        expect(isOAuthEnabled({ nodeEnv, oauthFlag })).toBe(expected);
    });
});
