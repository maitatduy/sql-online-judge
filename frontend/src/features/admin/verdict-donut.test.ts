import { describe, expect, it } from "vitest";

import { verdictDonutSegments } from "./verdict-donut";

describe("verdictDonutSegments", () => {
    it("xếp các cung nối tiếp nhau theo thứ tự verdict", () => {
        expect(
            verdictDonutSegments([
                { verdict: "AC", count: 50, percent: 50 },
                { verdict: "WA", count: 30, percent: 30 },
                { verdict: "RE", count: 20, percent: 20 },
            ]),
        ).toEqual([
            { verdict: "AC", percent: 50, offsetPercent: 0 },
            { verdict: "WA", percent: 30, offsetPercent: 50 },
            { verdict: "RE", percent: 20, offsetPercent: 80 },
        ]);
    });

    it("bỏ qua verdict không có bài nộp nhưng vẫn giữ đúng vị trí cung sau", () => {
        expect(
            verdictDonutSegments([
                { verdict: "AC", count: 60, percent: 60 },
                { verdict: "WA", count: 0, percent: 0 },
                { verdict: "RE", count: 40, percent: 40 },
            ]),
        ).toEqual([
            { verdict: "AC", percent: 60, offsetPercent: 0 },
            { verdict: "RE", percent: 40, offsetPercent: 60 },
        ]);
    });

    it("kéo các cung cho kín vòng tròn khi tổng phần trăm lệch 100 do làm tròn", () => {
        const segments = verdictDonutSegments([
            { verdict: "AC", count: 33, percent: 33 },
            { verdict: "WA", count: 33, percent: 33 },
            { verdict: "RE", count: 33, percent: 33 },
        ]);

        expect(segments.reduce((sum, segment) => sum + segment.percent, 0)).toBeCloseTo(100);
        expect(segments[2].offsetPercent).toBeCloseTo(66.67, 1);
    });

    it("không có cung nào khi chưa có bài nộp", () => {
        expect(verdictDonutSegments([])).toEqual([]);
        expect(verdictDonutSegments([{ verdict: "AC", count: 0, percent: 0 }])).toEqual([]);
    });
});
