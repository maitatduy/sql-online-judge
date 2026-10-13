import { describe, expect, it } from "vitest";

import { monthlyBars } from "./monthly-submissions";

describe("monthlyBars", () => {
    it("luôn trả về đủ 12 tháng theo thứ tự", () => {
        const bars = monthlyBars([{ month: 1, count: 100 }]);

        expect(bars.map((bar) => bar.month)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]);
    });

    it("tính chiều cao cột theo tháng nhiều bài nộp nhất", () => {
        const bars = monthlyBars([
            { month: 1, count: 50 },
            { month: 2, count: 200 },
            { month: 3, count: 100 },
        ]);

        expect(bars[0]).toEqual({ month: 1, count: 50, heightPercent: 25, isPeak: false });
        expect(bars[1]).toEqual({ month: 2, count: 200, heightPercent: 100, isPeak: true });
        expect(bars[2]).toEqual({ month: 3, count: 100, heightPercent: 50, isPeak: false });
    });

    it("để trống tháng chưa có số liệu", () => {
        const bars = monthlyBars([{ month: 1, count: 100 }]);

        expect(bars[11]).toEqual({ month: 12, count: null, heightPercent: 0, isPeak: false });
    });

    it("không có tháng cao nhất khi cả năm chưa có bài nộp", () => {
        const bars = monthlyBars([{ month: 1, count: 0 }]);

        expect(bars[0]).toEqual({ month: 1, count: 0, heightPercent: 0, isPeak: false });
        expect(bars.some((bar) => bar.isPeak)).toBe(false);
    });
});
