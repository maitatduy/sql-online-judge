import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { SEARCH_DEBOUNCE_MS, useDebouncedSearch } from "./useDebouncedSearch";

function renderSearch(appliedQuery: string, onSearch = vi.fn()) {
    const view = renderHook(({ query }) => useDebouncedSearch(query, onSearch), {
        initialProps: { query: appliedQuery },
    });
    return { ...view, onSearch };
}

describe("useDebouncedSearch", () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it("hiện từ khoá đang áp dụng khi mở", () => {
        const { result } = renderSearch("join");

        expect(result.current.searchText).toBe("join");
    });

    it("chỉ tìm sau khi ngừng gõ, với từ khoá đã bỏ khoảng trắng hai đầu", () => {
        const { result, onSearch } = renderSearch("");

        act(() => result.current.changeSearchText(" l"));
        act(() => result.current.changeSearchText(" lương "));
        act(() => vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS - 1));
        expect(onSearch).not.toHaveBeenCalled();

        act(() => vi.advanceTimersByTime(1));
        expect(onSearch).toHaveBeenCalledExactlyOnceWith("lương");
        expect(result.current.searchText).toBe(" lương ");
    });

    it("đồng bộ ô tìm khi từ khoá trên URL đổi từ nơi khác", () => {
        const { result, rerender } = renderSearch("join");

        rerender({ query: "" });

        expect(result.current.searchText).toBe("");
    });

    it("giữ nguyên chữ đang gõ khi URL chỉ là bản đã bỏ khoảng trắng của nó", () => {
        const { result, rerender } = renderSearch("");

        act(() => result.current.changeSearchText("cte "));
        rerender({ query: "cte" });

        expect(result.current.searchText).toBe("cte ");
    });

    it("xoá ô tìm và huỷ lượt tìm đang chờ", () => {
        const { result, onSearch } = renderSearch("");

        act(() => result.current.changeSearchText("null"));
        act(() => result.current.clearSearchText());
        act(() => vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS));

        expect(result.current.searchText).toBe("");
        expect(onSearch).not.toHaveBeenCalled();
    });
});
