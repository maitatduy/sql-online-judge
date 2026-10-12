import { describe, expect, it } from "vitest";

import { formatNumber } from "./format";

describe("formatNumber", () => {
    it.each([
        [36, "36"],
        [1806, "1.806"],
        [100000, "100.000"],
    ])("định dạng %d theo vi-VN thành %s", (value, expected) => {
        expect(formatNumber(value)).toBe(expected);
    });
});
