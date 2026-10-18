import { describe, expect, it } from "vitest";

import type { UserSession } from "@/features/admin/users/types";

import { sessionLabel } from "./SessionList";

const SESSION: UserSession = {
    id: "s1",
    browser: "Chrome",
    os: "Windows",
    city: "Hà Nội",
    lastActiveAt: "2026-10-18T04:57:00Z",
    isActiveNow: true,
    isUnusualLocation: false,
};

describe("sessionLabel", () => {
    it("ghi trình duyệt, hệ điều hành và thành phố", () => {
        expect(sessionLabel(SESSION)).toBe("Chrome · Windows · Hà Nội");
    });

    it("bỏ thành phố khi chưa có dữ liệu vị trí", () => {
        expect(sessionLabel({ ...SESSION, city: null })).toBe("Chrome · Windows");
    });
});
