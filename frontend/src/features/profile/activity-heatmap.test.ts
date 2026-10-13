import { describe, expect, it } from "vitest";

import {
    activityLevel,
    buildHeatmapWeeks,
    formatDateKey,
    vietnamDateKey,
} from "./activity-heatmap";

const TUESDAY_NOON_IN_VIETNAM = new Date("2026-10-13T05:00:00Z");

describe("buildHeatmapWeeks", () => {
    it("trải 52 tuần bắt đầu từ thứ Hai, tuần cuối dừng ở hôm nay", () => {
        const weeks = buildHeatmapWeeks([], TUESDAY_NOON_IN_VIETNAM);

        expect(weeks).toHaveLength(52);
        expect(weeks[0]).toHaveLength(7);
        expect(weeks[0][0].date).toBe("2025-10-20");
        expect(weeks[51].map((cell) => cell.date)).toEqual(["2026-10-12", "2026-10-13"]);
    });

    it("tuần cuối đủ 7 ngày khi hôm nay là Chủ nhật", () => {
        const weeks = buildHeatmapWeeks([], new Date("2026-10-18T05:00:00Z"));

        expect(weeks[51]).toHaveLength(7);
        expect(weeks[51][6].date).toBe("2026-10-18");
    });

    it("đi qua ranh giới năm không bỏ sót ngày nào", () => {
        const dates = buildHeatmapWeeks([], TUESDAY_NOON_IN_VIETNAM)
            .flat()
            .map((cell) => cell.date);

        expect(dates).toContain("2025-12-31");
        expect(dates).toContain("2026-01-01");
        expect(new Set(dates).size).toBe(51 * 7 + 2);
    });

    it("lấy hôm nay theo giờ Việt Nam chứ không theo UTC", () => {
        const justAfterMidnightInVietnam = new Date("2026-10-12T18:00:00Z");

        const weeks = buildHeatmapWeeks([], justAfterMidnightInVietnam);

        expect(weeks[51].at(-1)?.date).toBe("2026-10-13");
    });

    it("gắn số bài nộp vào đúng ngày, ngày không có dữ liệu là 0, bỏ ngày ngoài khoảng", () => {
        const weeks = buildHeatmapWeeks(
            [
                { date: "2026-10-13", count: 3 },
                { date: "2025-10-20", count: 5 },
                { date: "2024-01-01", count: 9 },
            ],
            TUESDAY_NOON_IN_VIETNAM,
        );
        const cells = weeks.flat();

        expect(weeks[0][0]).toEqual({ date: "2025-10-20", count: 5 });
        expect(weeks[51][1]).toEqual({ date: "2026-10-13", count: 3 });
        expect(weeks[51][0].count).toBe(0);
        expect(cells.some((cell) => cell.count === 9)).toBe(false);
    });
});

describe("activityLevel", () => {
    it.each([
        [0, 0],
        [1, 1],
        [2, 2],
        [3, 2],
        [4, 3],
        [5, 3],
        [6, 4],
        [40, 4],
    ])("%i bài nộp là mức %i", (count, level) => {
        expect(activityLevel(count)).toBe(level);
    });
});

describe("formatDateKey", () => {
    it("đổi ngày ISO sang kiểu ngày/tháng/năm", () => {
        expect(formatDateKey("2026-10-03")).toBe("03/10/2026");
    });
});

describe("vietnamDateKey", () => {
    it("lấy ngày theo giờ Việt Nam", () => {
        expect(vietnamDateKey(new Date("2026-09-12T18:30:00Z"))).toBe("2026-09-13");
    });
});
