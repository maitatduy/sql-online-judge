import { describe, expect, it } from "vitest";

import { getSafeNextPath } from "./safe-redirect";

describe("getSafeNextPath", () => {
    it.each([
        ["/problems", "/problems"],
        ["/contests/weekly-38?tab=standings", "/contests/weekly-38?tab=standings"],
        ["/learn#chapter-2", "/learn#chapter-2"],
    ])("giữ đường dẫn nội bộ %s", (next, expected) => {
        expect(getSafeNextPath(next)).toBe(expected);
    });

    it.each([
        ["không có giá trị", undefined],
        ["chuỗi rỗng", ""],
        ["URL tuyệt đối", "https://evil.example/login"],
        ["URL không có giao thức", "//evil.example"],
        ["dấu gạch chéo ngược", "/\\evil.example"],
        ["đường dẫn tương đối", "problems"],
        ["giao thức javascript", "javascript:alert(1)"],
        ["nhiều giá trị", ["/problems", "/contests"]],
        ["ký tự tab chen giữa hai dấu gạch chéo", "/\t/evil.example"],
        ["ký tự xuống dòng chen giữa hai dấu gạch chéo", "/\n/evil.example"],
        ["ký tự về đầu dòng chen giữa hai dấu gạch chéo", "/\r/evil.example"],
        ["khoảng trắng ở đầu", " /problems"],
        ["ký tự điều khiển trong đường dẫn", "/problems\u0000"],
    ])("về trang chủ khi next là %s", (_label, next) => {
        expect(getSafeNextPath(next)).toBe("/");
    });
});
