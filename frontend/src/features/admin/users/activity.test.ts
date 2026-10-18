import { describe, expect, it } from "vitest";

import { lastActiveLabel } from "./activity";

const NOW = new Date("2026-10-18T05:00:00Z");

describe("lastActiveLabel", () => {
    it("đang trực tuyến thì báo trực tuyến", () => {
        expect(lastActiveLabel({ isOnline: true, lastActiveAt: null }, NOW)).toBe(
            "Đang trực tuyến",
        );
    });

    it("chưa đăng nhập lần nào thì nói rõ, không hiện thời gian giả", () => {
        expect(lastActiveLabel({ isOnline: false, lastActiveAt: null }, NOW)).toBe(
            "Chưa đăng nhập lần nào",
        );
    });

    it("đã từng hoạt động thì hiện thời gian tương đối", () => {
        expect(
            lastActiveLabel({ isOnline: false, lastActiveAt: "2026-10-18T04:55:00Z" }, NOW),
        ).toBe("5 phút trước");
    });
});
