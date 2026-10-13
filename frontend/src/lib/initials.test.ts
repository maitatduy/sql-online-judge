import { describe, expect, it } from "vitest";

import { initialsOf } from "./initials";

describe("initialsOf", () => {
    it.each([
        ["Nguyễn Minh Anh", "MA"],
        ["Trần Bình", "TB"],
        ["  lê   văn   cường ", "VC"],
        ["Đức", "Đ"],
        ["", "?"],
    ])("tên %j cho chữ cái đầu %s", (displayName, initials) => {
        expect(initialsOf(displayName)).toBe(initials);
    });
});
