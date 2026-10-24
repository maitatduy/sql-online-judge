import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { NO_PROBLEM_FILTERS, type ProblemFilters } from "../problem-filters";
import { useProblems } from "./useProblems";

function renderProblems(initialFilters: ProblemFilters) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderHook(({ filters }) => useProblems(filters), {
        initialProps: { filters: initialFilters },
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useProblems", () => {
    it("tải danh sách bài theo bộ lọc", async () => {
        const { result } = renderProblems({ ...NO_PROBLEM_FILTERS, difficulty: "HARD" });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data?.items.every((problem) => problem.difficulty === "HARD")).toBe(
            true,
        );
    });

    it("giữ trang cũ trong lúc tải trang mới để bảng không nháy", async () => {
        const { result, rerender } = renderProblems(NO_PROBLEM_FILTERS);
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        rerender({ filters: { ...NO_PROBLEM_FILTERS, page: 2 } });

        expect(result.current.data?.page).toBe(1);
        await waitFor(() => expect(result.current.data?.page).toBe(2));
    });
});
