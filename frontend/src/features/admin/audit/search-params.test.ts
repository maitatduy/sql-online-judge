import { describe, expect, it } from "vitest";

import { auditHref, auditQueryOf, NO_AUDIT_FILTERS, readAuditFilters } from "./search-params";

const NOW = new Date("2026-10-25T05:30:00Z");
const ALL_TIME = { ...NO_AUDIT_FILTERS, period: null };

describe("bộ lọc nhật ký trên URL", () => {
    it("đọc đủ bộ lọc hợp lệ và bỏ qua giá trị lạ", () => {
        expect(
            readAuditFilters(
                new URLSearchParams(
                    "user=viet.luu&actor=admin.huy&category=ACCOUNT&q=%20spam%20&period=30d&page=3",
                ),
            ),
        ).toEqual({
            user: "viet.luu",
            actor: "admin.huy",
            category: "ACCOUNT",
            q: "spam",
            period: "30d",
            page: 3,
        });
        expect(
            readAuditFilters(new URLSearchParams("category=BILLING&period=year&page=-1&q=%20")),
        ).toEqual(NO_AUDIT_FILTERS);
        expect(readAuditFilters(new URLSearchParams("category=PROBLEM")).category).toBeNull();
    });

    it("mặc định 7 ngày qua, nhưng xem nhật ký của một người thì mặc định mọi lúc", () => {
        expect(readAuditFilters(new URLSearchParams("")).period).toBe("7d");
        expect(readAuditFilters(new URLSearchParams("user=viet.luu")).period).toBeNull();
        expect(readAuditFilters(new URLSearchParams("period=all")).period).toBeNull();
    });

    it("ghi lại URL chỉ với bộ lọc khác mặc định", () => {
        expect(auditHref(NO_AUDIT_FILTERS)).toBe("/admin/audit-log");
        expect(auditHref(ALL_TIME)).toBe("/admin/audit-log?period=all");
        expect(
            auditHref({ ...ALL_TIME, user: "viet.luu", category: "ACCOUNT", q: "spam", page: 2 }),
        ).toBe("/admin/audit-log?user=viet.luu&category=ACCOUNT&q=spam&page=2");
        expect(auditHref({ ...NO_AUDIT_FILTERS, user: "viet.luu" })).toBe(
            "/admin/audit-log?user=viet.luu&period=7d",
        );
    });

    it("đổi bộ lọc thành tham số API: khoảng thời gian thành mốc from theo giờ Việt Nam", () => {
        expect(auditQueryOf({ ...NO_AUDIT_FILTERS, period: "today" }, NOW)).toEqual({
            from: "2026-10-24T17:00:00.000Z",
        });
        expect(auditQueryOf(NO_AUDIT_FILTERS, NOW).from).toBe("2026-10-18T05:30:00.000Z");
        expect(auditQueryOf({ ...NO_AUDIT_FILTERS, period: "30d" }, NOW).from).toBe(
            "2026-09-25T05:30:00.000Z",
        );
        expect(
            auditQueryOf(
                {
                    ...ALL_TIME,
                    user: "viet.luu",
                    actor: "admin.huy",
                    category: "ACCOUNT",
                    q: "spam",
                },
                NOW,
            ),
        ).toEqual({ user: "viet.luu", actor: "admin.huy", category: "ACCOUNT", q: "spam" });
    });
});
