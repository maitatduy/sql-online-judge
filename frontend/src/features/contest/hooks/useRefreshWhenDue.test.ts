import { renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { contestRefetchInterval, useRefreshWhenDue } from "./useRefreshWhenDue";

const NOW = new Date("2026-11-28T04:39:00Z");

describe("useRefreshWhenDue", () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(NOW);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it("gọi tải lại ngay sau khi tới giờ, không gọi sớm hơn", () => {
        const onDue = vi.fn();
        renderHook(() => useRefreshWhenDue("2026-11-28T04:39:10Z", onDue));

        vi.advanceTimersByTime(9_000);
        expect(onDue).not.toHaveBeenCalled();
        vi.advanceTimersByTime(2_000);
        expect(onDue).toHaveBeenCalledTimes(1);
    });

    it("không hẹn giờ khi không có mốc thời gian", () => {
        const onDue = vi.fn();
        renderHook(() => useRefreshWhenDue(undefined, onDue));

        vi.advanceTimersByTime(60_000);
        expect(onDue).not.toHaveBeenCalled();
    });
});

describe("contestRefetchInterval", () => {
    const upcoming = {
        phase: "UPCOMING" as const,
        startAt: "2026-11-28T04:39:30Z",
        endAt: "2026-11-28T06:39:30Z",
    };

    it("chưa tới giờ thì không hỏi lại", () => {
        expect(contestRefetchInterval(upcoming, NOW)).toBe(false);
    });

    it("đã qua giờ bắt đầu mà server vẫn báo sắp diễn ra thì hỏi lại mỗi 2 giây", () => {
        expect(contestRefetchInterval(upcoming, new Date("2026-11-28T04:39:31Z"))).toBe(2_000);
    });

    it("đang diễn ra mà đã qua giờ kết thúc thì hỏi lại, đã kết thúc thì thôi", () => {
        const running = { ...upcoming, phase: "RUNNING" as const };
        expect(contestRefetchInterval(running, new Date("2026-11-28T06:40:00Z"))).toBe(2_000);
        expect(contestRefetchInterval({ ...upcoming, phase: "FINISHED" as const }, NOW)).toBe(
            false,
        );
    });
});
