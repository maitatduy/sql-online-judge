import { describe, expect, it } from "vitest";

import { parseCsv, rowsFromCsv, rowsToCsv } from "./csv";

describe("csv", () => {
    it("đọc ô có nháy kép, dấu phẩy, xuống dòng và nháy kép lồng", () => {
        expect(
            parseCsv('id,name\r\n1,"Nguyễn, An"\n2,"Dòng một\nDòng hai"\n3,"Nói ""chào"""\n'),
        ).toEqual([
            ["id", "name"],
            ["1", "Nguyễn, An"],
            ["2", "Dòng một\nDòng hai"],
            ["3", 'Nói "chào"'],
        ]);
    });

    it("CSV trống không có dòng nào", () => {
        expect(parseCsv("")).toEqual([]);
    });

    it("đổi CSV thành các dòng theo đúng thứ tự cột, ô trống là null", () => {
        expect(rowsFromCsv("name,id\nAn,1\n,2\n", ["id", "name"])).toEqual({
            rows: [
                ["1", "An"],
                ["2", null],
            ],
            error: null,
        });
    });

    it("báo lỗi khi dòng tiêu đề không khớp cột của bảng", () => {
        expect(rowsFromCsv("id,title\n1,A\n", ["id", "name"])).toEqual({
            rows: [],
            error: "Dòng tiêu đề phải gồm các cột: id, name",
        });
    });

    it("ghi các dòng thành CSV, ô null để trống, ô có ký tự đặc biệt được bọc nháy kép", () => {
        expect(
            rowsToCsv(
                ["id", "name"],
                [
                    ["1", "Nguyễn, An"],
                    ["2", null],
                    ["3", 'Nói "chào"'],
                ],
            ),
        ).toBe('id,name\n1,"Nguyễn, An"\n2,\n3,"Nói ""chào"""\n');
    });
});
