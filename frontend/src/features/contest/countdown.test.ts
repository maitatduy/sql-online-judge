import { describe, expect, it } from "vitest";

import { countdownParts, formatContestDay } from "./countdown";

const NOW = new Date("2026-11-22T12:00:00Z");

describe("countdownParts", () => {
    it("tách thời gian còn lại thành ngày, giờ, phút, giây có hai chữ số", () => {
        expect(countdownParts("2026-11-24T15:41:08Z", NOW)).toEqual([
            { value: "02", unit: "ngày" },
            { value: "03", unit: "giờ" },
            { value: "41", unit: "phút" },
            { value: "08", unit: "giây" },
        ]);
    });

    it("bỏ ô ngày khi còn dưới một ngày", () => {
        expect(countdownParts("2026-11-22T13:12:40Z", NOW)).toEqual([
            { value: "01", unit: "giờ" },
            { value: "12", unit: "phút" },
            { value: "40", unit: "giây" },
        ]);
    });

    it("dừng ở 00 khi đã qua mốc", () => {
        expect(countdownParts("2026-11-22T11:00:00Z", NOW).map((part) => part.value)).toEqual([
            "00",
            "00",
            "00",
        ]);
    });
});

describe("formatContestDay", () => {
    it("ghi thứ, ngày theo giờ Việt Nam", () => {
        expect(formatContestDay("2026-10-17T12:30:00Z")).toEqual({
            weekday: "Thứ 7",
            day: "17",
            month: "Th10",
        });
        expect(formatContestDay("2026-10-17T17:30:00Z").weekday).toBe("CN");
    });
});
