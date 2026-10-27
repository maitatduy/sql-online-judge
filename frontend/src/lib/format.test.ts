import { describe, expect, it } from "vitest";

import { formatDate, formatNumber, formatPercent, formatRelativeTime, formatTime } from "./format";

describe("formatDate", () => {
    it("định dạng ngày theo dd/MM/yyyy", () => {
        expect(formatDate("2025-03-14T03:00:00Z")).toBe("14/03/2025");
    });

    it("đổi sang giờ Việt Nam trước khi lấy ngày", () => {
        expect(formatDate("2026-10-10T18:30:00Z")).toBe("11/10/2026");
    });
});

describe("formatTime", () => {
    it("đổi giờ UTC sang giờ phút Việt Nam", () => {
        expect(formatTime("2026-10-10T13:06:00Z")).toBe("20:06");
    });

    it("dùng đồng hồ 24 giờ khi qua nửa đêm", () => {
        expect(formatTime("2026-10-10T17:05:00Z")).toBe("00:05");
    });
});

describe("formatNumber", () => {
    it.each([
        [36, "36"],
        [1806, "1.806"],
        [100000, "100.000"],
    ])("định dạng %d theo vi-VN thành %s", (value, expected) => {
        expect(formatNumber(value)).toBe(expected);
    });
});

describe("formatPercent", () => {
    it("làm tròn một chữ số thập phân, dùng dấu phẩy", () => {
        expect(formatPercent(41.25)).toBe("41,3%");
    });

    it("bỏ phần thập phân khi tròn", () => {
        expect(formatPercent(50)).toBe("50%");
    });
});

describe("formatRelativeTime", () => {
    const now = new Date("2026-10-13T05:00:00Z");

    it.each([
        ["2026-10-13T04:59:40Z", "Vừa xong"],
        ["2026-10-13T04:55:00Z", "5 phút trước"],
        ["2026-10-13T03:00:00Z", "2 giờ trước"],
        ["2026-10-11T05:00:00Z", "2 ngày trước"],
        ["2026-10-06T05:00:00Z", "1 tuần trước"],
        ["2026-08-10T05:00:00Z", "2 tháng trước"],
        ["2025-09-13T05:00:00Z", "1 năm trước"],
    ])("%s so với hiện tại là %s", (date, expected) => {
        expect(formatRelativeTime(date, now)).toBe(expected);
    });

    it("coi thời điểm lệch về tương lai do đồng hồ máy là vừa xong", () => {
        expect(formatRelativeTime("2026-10-13T05:00:30Z", now)).toBe("Vừa xong");
    });
});
