import { describe, expect, it } from "vitest";

import { invalidEmailsOf, parseEmailList } from "./email-list";

describe("parseEmailList", () => {
    it("tách theo dòng và dấu phẩy, bỏ khoảng trắng và dòng trống", () => {
        expect(parseEmailList(" a@example.com \n\nb@example.com, c@example.com ,")).toEqual([
            "a@example.com",
            "b@example.com",
            "c@example.com",
        ]);
    });

    it("bỏ email trùng, không phân biệt hoa thường, giữ lần xuất hiện đầu", () => {
        expect(parseEmailList("A@example.com\na@EXAMPLE.com\nb@example.com")).toEqual([
            "A@example.com",
            "b@example.com",
        ]);
    });

    it("trả về danh sách rỗng khi không nhập gì", () => {
        expect(parseEmailList("  \n , ")).toEqual([]);
    });
});

describe("invalidEmailsOf", () => {
    it("chỉ ra các email sai định dạng", () => {
        expect(
            invalidEmailsOf(["a@example.com", "khong-phai-email", "b@", "c@example.vn"]),
        ).toEqual(["khong-phai-email", "b@"]);
    });
});
