import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { Dialect } from "@/lib/dialect";

import { DRAFT_SAVE_DELAY_MS, useEditorDraft } from "./useEditorDraft";

const PROBLEM_ID = "0192f3b0-0000-7000-8000-0000000000b8";
const DRAFT_KEY = `draft:${PROBLEM_ID}:POSTGRESQL`;
const STARTER_CODE = "SELECT *\nFROM employees;";

function renderDraft(
    problemId = PROBLEM_ID,
    starterCode = STARTER_CODE,
    dialect: Dialect = "POSTGRESQL",
) {
    return renderHook(({ id, starter, selected }) => useEditorDraft(id, selected, starter), {
        initialProps: { id: problemId, starter: starterCode, selected: dialect },
    });
}

describe("useEditorDraft", () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date("2026-10-22T12:24:00Z"));
        localStorage.clear();
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
        localStorage.clear();
    });

    it("mở mã khởi đầu khi chưa có nháp", () => {
        const { result } = renderDraft();

        expect(result.current.code).toBe(STARTER_CODE);
        expect(result.current.savedAt).toBeNull();
    });

    it("mở lại nháp đã lưu của đúng bài", () => {
        localStorage.setItem(DRAFT_KEY, "SELECT 1;");

        const { result } = renderDraft();

        expect(result.current.code).toBe("SELECT 1;");
    });

    it("lưu nháp sau khi ngừng gõ và ghi thời điểm lưu", () => {
        const { result } = renderDraft();

        act(() => result.current.changeCode("SELECT name"));
        act(() => result.current.changeCode("SELECT name FROM employees;"));
        act(() => vi.advanceTimersByTime(DRAFT_SAVE_DELAY_MS - 1));
        expect(localStorage.getItem(DRAFT_KEY)).toBeNull();

        act(() => vi.advanceTimersByTime(1));
        expect(localStorage.getItem(DRAFT_KEY)).toBe("SELECT name FROM employees;");
        expect(result.current.savedAt).toEqual(new Date("2026-10-22T12:24:00.500Z"));
    });

    it("tách nháp theo từng bài khi chuyển bài", () => {
        localStorage.setItem("draft:other-problem:POSTGRESQL", "SELECT 'khác';");
        const { result, rerender } = renderDraft();
        act(() => result.current.changeCode("SELECT 184;"));
        act(() => vi.advanceTimersByTime(DRAFT_SAVE_DELAY_MS));

        rerender({ id: "other-problem", starter: "SELECT 1;", selected: "POSTGRESQL" });

        expect(result.current.code).toBe("SELECT 'khác';");
        expect(localStorage.getItem(DRAFT_KEY)).toBe("SELECT 184;");
    });

    it("tách nháp theo dialect và lưu ngay phần đang gõ khi đổi dialect", () => {
        localStorage.setItem(`draft:${PROBLEM_ID}:MYSQL`, "SELECT 'mysql';");
        const { result, rerender } = renderDraft();
        act(() => result.current.changeCode("SELECT 'postgres';"));

        rerender({ id: PROBLEM_ID, starter: STARTER_CODE, selected: "MYSQL" });

        expect(result.current.code).toBe("SELECT 'mysql';");
        expect(localStorage.getItem(DRAFT_KEY)).toBe("SELECT 'postgres';");

        rerender({ id: PROBLEM_ID, starter: STARTER_CODE, selected: "SQLITE" });

        expect(result.current.code).toBe(STARTER_CODE);
    });

    it("đặt lại về mã khởi đầu và xoá nháp", () => {
        localStorage.setItem(DRAFT_KEY, "SELECT 1;");
        const { result } = renderDraft();

        act(() => result.current.resetToStarter());

        expect(result.current.code).toBe(STARTER_CODE);
        expect(localStorage.getItem(DRAFT_KEY)).toBeNull();
        expect(result.current.savedAt).toBeNull();
    });

    it("vẫn soạn được khi trình duyệt chặn localStorage, chỉ là không lưu nháp", () => {
        vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
            throw new DOMException("blocked", "SecurityError");
        });
        vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
            throw new DOMException("blocked", "SecurityError");
        });
        const { result } = renderDraft();

        act(() => result.current.changeCode("SELECT 2;"));
        act(() => vi.advanceTimersByTime(DRAFT_SAVE_DELAY_MS));

        expect(result.current.code).toBe("SELECT 2;");
        expect(result.current.savedAt).toBeNull();
    });
});
