import { describe, expect, it } from "vitest";

import { getPasswordRules } from "./password-strength";

function metRules(password: string): boolean[] {
    return getPasswordRules(password).map((rule) => rule.isMet);
}

describe("getPasswordRules", () => {
    it.each([
        ["", [false, false, false, false]],
        ["abcdefgh", [true, false, false, false]],
        ["abcdefg1", [true, true, false, false]],
        ["mậtkhẩu12", [true, true, false, false]],
        ["abcdefg1!", [true, true, true, false]],
        ["abcdefghij1!", [true, true, true, true]],
        ["a1!", [false, true, true, false]],
    ])("%s đạt các tiêu chí %j", (password, expected) => {
        expect(metRules(password)).toEqual(expected);
    });

    it("ghi rõ độ dài tối thiểu và độ dài khuyên dùng", () => {
        expect(getPasswordRules("").map((rule) => rule.label)).toEqual([
            "Ít nhất 8 ký tự",
            "Có cả chữ và số",
            "Có ký tự đặc biệt",
            "Từ 12 ký tự trở lên",
        ]);
    });
});
