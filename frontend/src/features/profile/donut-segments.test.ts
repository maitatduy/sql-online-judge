import { describe, expect, it } from "vitest";

import { MINH_ANH_PROFILE, NEWBIE_PROFILE } from "@/mocks/profile-fixtures";

import { donutSegments } from "./donut-segments";

describe("donutSegments", () => {
    it("chia vòng theo tỉ lệ số bài đã giải trên tổng số bài, nối tiếp nhau", () => {
        const segments = donutSegments(MINH_ANH_PROFILE.stats.solvedByDifficulty);

        expect(segments.map((segment) => segment.difficulty)).toEqual(["EASY", "MEDIUM", "HARD"]);
        expect(segments[0].percent).toBeCloseTo((42 / 330) * 100);
        expect(segments[0].offsetPercent).toBe(0);
        expect(segments[1].percent).toBeCloseTo((18 / 330) * 100);
        expect(segments[1].offsetPercent).toBeCloseTo((42 / 330) * 100);
        expect(segments[2].percent).toBeCloseTo((3 / 330) * 100);
        expect(segments[2].offsetPercent).toBeCloseTo((60 / 330) * 100);
    });

    it("bỏ độ khó chưa giải bài nào", () => {
        const segments = donutSegments({
            EASY: { solved: 5, total: 10 },
            MEDIUM: { solved: 0, total: 10 },
            HARD: { solved: 5, total: 20 },
        });

        expect(segments.map((segment) => segment.difficulty)).toEqual(["EASY", "HARD"]);
        expect(segments[1].offsetPercent).toBeCloseTo(12.5);
    });

    it("không có đoạn nào khi chưa giải bài nào", () => {
        expect(donutSegments(NEWBIE_PROFILE.stats.solvedByDifficulty)).toEqual([]);
    });

    it("không chia cho 0 khi hệ thống chưa có bài", () => {
        const segments = donutSegments({
            EASY: { solved: 0, total: 0 },
            MEDIUM: { solved: 0, total: 0 },
            HARD: { solved: 0, total: 0 },
        });

        expect(segments).toEqual([]);
    });
});
