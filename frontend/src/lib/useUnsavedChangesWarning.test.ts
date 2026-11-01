import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { useUnsavedChangesWarning } from "./useUnsavedChangesWarning";

function fireBeforeUnload(): boolean {
    const event = new Event("beforeunload", { cancelable: true });
    window.dispatchEvent(event);
    return event.defaultPrevented;
}

describe("useUnsavedChangesWarning", () => {
    it("chặn rời trang khi còn thay đổi chưa lưu", () => {
        renderHook(() => useUnsavedChangesWarning(true));

        expect(fireBeforeUnload()).toBe(true);
    });

    it("không chặn khi đã lưu hết, và gỡ cảnh báo khi đã lưu xong", () => {
        const { rerender } = renderHook(({ dirty }) => useUnsavedChangesWarning(dirty), {
            initialProps: { dirty: true },
        });

        rerender({ dirty: false });

        expect(fireBeforeUnload()).toBe(false);
    });
});
