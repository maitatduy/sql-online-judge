import { describe, expect, it } from "vitest";

import { pageRangeLabel, pageWindow } from "./pagination";

describe("pageWindow", () => {
    it.each([
        [1, 4, [1, 2, 3, 4]],
        [1, 12, [1, 2, 3, 4, 5]],
        [6, 12, [4, 5, 6, 7, 8]],
        [12, 12, [8, 9, 10, 11, 12]],
        [1, 0, []],
    ])("trang %d trên %d hiện các nút %j", (page, totalPages, expected) => {
        expect(pageWindow(page, totalPages)).toEqual(expected);
    });
});

describe("pageRangeLabel", () => {
    it("nêu khoảng dòng của trang đầu", () => {
        expect(pageRangeLabel({ page: 1, size: 20, totalItems: 52318 })).toBe("1-20 trong 52.318");
    });

    it("cắt khoảng dòng ở trang cuối", () => {
        expect(pageRangeLabel({ page: 4, size: 20, totalItems: 62 })).toBe("61-62 trong 62");
    });

    it("báo không có kết quả khi danh sách rỗng", () => {
        expect(pageRangeLabel({ page: 1, size: 20, totalItems: 0 })).toBe("0 kết quả");
    });

    it("thêm đơn vị khi được truyền vào", () => {
        expect(pageRangeLabel({ page: 1, size: 12, totalItems: 486 }, "thao tác")).toBe(
            "1-12 trong 486 thao tác",
        );
        expect(pageRangeLabel({ page: 1, size: 12, totalItems: 0 }, "thao tác")).toBe("0 thao tác");
    });
});
