import { describe, expect, it } from "vitest";

import { toCsv } from "./csv";

describe("toCsv", () => {
    it("nối các ô bằng dấu phẩy và kết thúc mỗi dòng bằng xuống dòng", () => {
        expect(
            toCsv([
                ["username", "role"],
                ["viet.luu", "SETTER"],
            ]),
        ).toBe("username,role\nviet.luu,SETTER\n");
    });

    it.each([
        ["dấu phẩy", "Lưu, Việt", '"Lưu, Việt"'],
        ["nháy kép", 'Việt "Lưu"', '"Việt ""Lưu"""'],
        ["xuống dòng", "dòng 1\ndòng 2", '"dòng 1\ndòng 2"'],
        ["về đầu dòng", "dòng 1\r\ndòng 2", '"dòng 1\r\ndòng 2"'],
    ])("bọc nháy kép ô có %s", (_, value, escaped) => {
        expect(toCsv([[value, "x"]])).toBe(`${escaped},x\n`);
    });

    it("giữ nguyên ô không có ký tự đặc biệt", () => {
        expect(toCsv([["2026-10-20T03:00:00Z", ""]])).toBe("2026-10-20T03:00:00Z,\n");
    });
});
