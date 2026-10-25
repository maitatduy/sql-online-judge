import { describe, expect, it } from "vitest";

import { auditHref, auditQueryOf, NO_AUDIT_FILTERS, readAuditFilters } from "./search-params";

const NOW = new Date("2026-10-25T05:30:00Z");

describe("bộ lọc nhật ký trên URL", () => {
    it("đọc đủ bộ lọc hợp lệ và bỏ qua giá trị lạ", () => {
        expect(
            readAuditFilters(
                new URLSearchParams("user=viet.luu&actor=admin.huy&action=BAN&period=7d&page=3"),
            ),
        ).toEqual({ user: "viet.luu", actor: "admin.huy", action: "BAN", period: "7d", page: 3 });
        expect(
            readAuditFilters(new URLSearchParams("action=DROP&period=year&page=-1&actor=%20")),
        ).toEqual(NO_AUDIT_FILTERS);
    });

    it("ghi lại URL chỉ với bộ lọc đang bật", () => {
        expect(auditHref(NO_AUDIT_FILTERS)).toBe("/admin/audit-log");
        expect(auditHref({ ...NO_AUDIT_FILTERS, user: "viet.luu", action: "BAN", page: 2 })).toBe(
            "/admin/audit-log?user=viet.luu&action=BAN&page=2",
        );
    });

    it("đổi khoảng thời gian thành mốc from: hôm nay tính từ 0 giờ giờ Việt Nam", () => {
        expect(auditQueryOf({ ...NO_AUDIT_FILTERS, period: "today" }, NOW)).toEqual({
            from: "2026-10-24T17:00:00.000Z",
        });
        expect(auditQueryOf({ ...NO_AUDIT_FILTERS, period: "7d" }, NOW).from).toBe(
            "2026-10-18T05:30:00.000Z",
        );
        expect(auditQueryOf({ ...NO_AUDIT_FILTERS, period: "30d" }, NOW).from).toBe(
            "2026-09-25T05:30:00.000Z",
        );
        expect(
            auditQueryOf({ ...NO_AUDIT_FILTERS, user: "viet.luu", actor: "admin.huy" }, NOW),
        ).toEqual({ user: "viet.luu", actor: "admin.huy" });
    });
});
