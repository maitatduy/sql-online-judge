import { describe, expect, it } from "vitest";

import { formatVietnamTime, formatWaitDuration } from "./format";

describe("formatWaitDuration", () => {
    it("hiện một chữ số thập phân với dấu phẩy", () => {
        expect(formatWaitDuration(3800)).toBe("3,8 giây");
    });

    it("bỏ phần thập phân khi tròn giây", () => {
        expect(formatWaitDuration(12_000)).toBe("12 giây");
    });

    it("làm tròn tới một chữ số thập phân", () => {
        expect(formatWaitDuration(2149)).toBe("2,1 giây");
    });
});

describe("formatVietnamTime", () => {
    it("đổi giờ UTC sang giờ phút Việt Nam", () => {
        expect(formatVietnamTime("2026-10-10T13:06:00Z")).toBe("20:06");
    });

    it("dùng đồng hồ 24 giờ khi qua nửa đêm", () => {
        expect(formatVietnamTime("2026-10-10T17:05:00Z")).toBe("00:05");
    });
});
