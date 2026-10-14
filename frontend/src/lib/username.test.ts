import { describe, expect, it } from "vitest";

import { isReservedUsername } from "./username";

describe("isReservedUsername", () => {
    it.each(["availability", "me"])("coi '%s' là tên dành riêng", (username) => {
        expect(isReservedUsername(username)).toBe(true);
    });

    it.each(["viet.luu", "availability.2", "mei"])("cho phép '%s'", (username) => {
        expect(isReservedUsername(username)).toBe(false);
    });
});
