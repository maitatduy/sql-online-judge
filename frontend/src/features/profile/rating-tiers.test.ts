import { describe, expect, it } from "vitest";

import { peakRating, ratingAxisDomain, ratingChange, tierRangeLabel } from "./rating-tiers";

describe("peakRating", () => {
    it("trả rating cao nhất trong lịch sử", () => {
        expect(peakRating([{ rating: 1320 }, { rating: 1487 }, { rating: 1404 }])).toBe(1487);
    });
});

describe("ratingAxisDomain", () => {
    it("chừa khoảng dưới điểm thấp nhất và vươn tới hạng kế tiếp phía trên", () => {
        expect(ratingAxisDomain([{ rating: 1320 }, { rating: 1487 }])).toEqual([1250, 1700]);
    });

    it("chỉ chừa khoảng phía trên khi đã ở hạng cao nhất", () => {
        expect(ratingAxisDomain([{ rating: 1610 }, { rating: 1702 }])).toEqual([1550, 1850]);
    });
});

describe("tierRangeLabel", () => {
    it("ghi khoảng rating bằng gạch nối", () => {
        expect(tierRangeLabel({ title: "Chuyên viên", minRating: 1400, maxRating: 1599 })).toBe(
            "Chuyên viên 1.400-1.599",
        );
    });

    it("ghi ngưỡng dưới khi hạng không có trần", () => {
        expect(tierRangeLabel({ title: "Chuyên gia", minRating: 1600 })).toBe("Chuyên gia ≥ 1.600");
    });
});

describe("ratingChange", () => {
    const points = [{ rating: 1320 }, { rating: 1352 }, { rating: 1338 }];

    it("tính mức tăng so với contest liền trước", () => {
        expect(ratingChange(points, 1)).toBe(32);
    });

    it("tính mức giảm thành số âm", () => {
        expect(ratingChange(points, 2)).toBe(-14);
    });

    it("contest đầu tiên không có mức thay đổi", () => {
        expect(ratingChange(points, 0)).toBeNull();
    });
});
