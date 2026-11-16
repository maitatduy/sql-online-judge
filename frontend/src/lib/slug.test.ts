import { describe, expect, it } from "vitest";

import { toSlug } from "./slug";

describe("toSlug", () => {
    it("bỏ dấu tiếng Việt, đổi đ thành d và nối bằng gạch ngang", () => {
        expect(toSlug("Tổng lũy kế theo tháng")).toBe("tong-luy-ke-theo-thang");
        expect(toSlug("Đếm đơn hàng")).toBe("dem-don-hang");
    });

    it("gộp ký tự đặc biệt và bỏ gạch ngang ở hai đầu", () => {
        expect(toSlug("  ROW_NUMBER() & RANK!  ")).toBe("row-number-rank");
    });

    it("cắt còn tối đa 160 ký tự", () => {
        expect(toSlug("a ".repeat(200))).toHaveLength(159);
    });

    it("trả chuỗi rỗng khi không còn ký tự hợp lệ", () => {
        expect(toSlug("!!!")).toBe("");
    });
});
