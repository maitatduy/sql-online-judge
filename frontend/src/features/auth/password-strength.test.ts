import { describe, expect, it } from "vitest";

import { getPasswordLevel, getPasswordScore } from "./password-strength";

describe("getPasswordScore", () => {
    it.each([
        ["", 0],
        ["abcdefgh", 1],
        ["abcdefg1", 2],
        ["mậtkhẩu12", 2],
        ["abcdefg1!", 3],
        ["abcdefghij1!", 4],
    ])("%s được %d điểm", (password, score) => {
        expect(getPasswordScore(password)).toBe(score);
    });

    it("mật khẩu dưới 8 ký tự không vượt quá 1 điểm dù có đủ loại ký tự", () => {
        expect(getPasswordScore("a1!")).toBe(1);
    });
});

describe("getPasswordLevel", () => {
    it.each([
        ["", "EMPTY"],
        ["a1!", "WEAK"],
        ["abcdefgh", "WEAK"],
        ["abcdefg1", "FAIR"],
        ["abcdefg1!", "STRONG"],
        ["abcdefghij1!", "STRONG"],
    ])("%s ở mức %s", (password, level) => {
        expect(getPasswordLevel(password)).toBe(level);
    });
});
