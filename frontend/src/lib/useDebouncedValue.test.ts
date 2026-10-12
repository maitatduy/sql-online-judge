import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { useDebouncedValue } from "./useDebouncedValue";

describe("useDebouncedValue", () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it("trả về giá trị ban đầu ngay lập tức", () => {
        const { result } = renderHook(() => useDebouncedValue("a", 300));

        expect(result.current).toBe("a");
    });

    it("chỉ cập nhật sau khi giá trị đứng yên đủ thời gian chờ", () => {
        const { result, rerender } = renderHook(({ value }) => useDebouncedValue(value, 300), {
            initialProps: { value: "a" },
        });

        rerender({ value: "ab" });
        act(() => {
            vi.advanceTimersByTime(299);
        });
        expect(result.current).toBe("a");

        act(() => {
            vi.advanceTimersByTime(1);
        });
        expect(result.current).toBe("ab");
    });

    it("gõ tiếp thì tính lại thời gian chờ từ đầu", () => {
        const { result, rerender } = renderHook(({ value }) => useDebouncedValue(value, 300), {
            initialProps: { value: "a" },
        });

        rerender({ value: "ab" });
        act(() => {
            vi.advanceTimersByTime(200);
        });
        rerender({ value: "abc" });
        act(() => {
            vi.advanceTimersByTime(200);
        });
        expect(result.current).toBe("a");

        act(() => {
            vi.advanceTimersByTime(100);
        });
        expect(result.current).toBe("abc");
    });
});
