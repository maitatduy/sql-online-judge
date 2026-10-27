import { describe, expect, it } from "vitest";

import { formatAuditMoment, formatAuditTime, formatWaitDuration } from "./format";

describe("formatWaitDuration", () => {
    it("hiện một chữ số thập phân với dấu phẩy", () => {
        expect(formatWaitDuration(3800)).toBe("3,8 giây");
    });

    it("bỏ phần thập phân khi tròn giây", () => {
        expect(formatWaitDuration(12_000)).toBe("12 giây");
    });

    it("làm tròn tới một chữ số thập phân", () => {
        expect(formatWaitDuration(2149)).toBe("2,1 giây");
    });
});

describe("formatAuditTime", () => {
    it("ghép ngày và giờ Việt Nam kèm giây, sang ngày mới khi qua nửa đêm", () => {
        expect(formatAuditTime("2026-10-13T07:05:12Z")).toBe("13/10/2026 14:05:12");
        expect(formatAuditTime("2026-10-10T17:05:00Z")).toBe("11/10/2026 00:05:00");
    });
});

describe("formatAuditMoment", () => {
    const now = new Date("2026-10-13T09:00:00Z");

    it("gọi tên hôm nay và hôm qua theo giờ Việt Nam", () => {
        expect(formatAuditMoment("2026-10-13T08:41:00Z", now)).toBe("Hôm nay 15:41");
        expect(formatAuditMoment("2026-10-12T17:10:00Z", now)).toBe("Hôm nay 00:10");
        expect(formatAuditMoment("2026-10-12T14:45:00Z", now)).toBe("Hôm qua 21:45");
    });

    it("ngày cũ hơn ghi thứ và ngày tháng, khác năm thì ghi đủ năm", () => {
        expect(formatAuditMoment("2026-10-11T01:10:00Z", now)).toBe("CN 11/10 08:10");
        expect(formatAuditMoment("2026-10-10T14:02:00Z", now)).toBe("T7 10/10 21:02");
        expect(formatAuditMoment("2025-12-29T02:00:00Z", now)).toBe("29/12/2025 09:00");
    });
});
