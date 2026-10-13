import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { useRecentSubmissions } from "./useRecentSubmissions";

function renderRecentSubmissions(username: string) {
    const queryClient = new QueryClient();
    return renderHook(() => useRecentSubmissions(username), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useRecentSubmissions", () => {
    it("tải trang đầu gồm 5 bài nộp mới nhất, không kèm code", async () => {
        const { result } = renderRecentSubmissions("minh.anh");

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.submissions).toHaveLength(5);
        expect(result.current.submissions[0]).toMatchObject({
            verdict: "AC",
            problemNumber: 184,
            problemTitle: "Lương cao nhất mỗi phòng ban",
            dialect: "POSTGRESQL",
        });
        expect(result.current.submissions[0]).not.toHaveProperty("code");
        expect(result.current.hasNextPage).toBe(true);
    });

    it("tải tiếp bằng cursor cho tới khi hết", async () => {
        const { result } = renderRecentSubmissions("minh.anh");
        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        await act(() => result.current.fetchNextPage());
        await waitFor(() => expect(result.current.submissions).toHaveLength(10));
        expect(result.current.hasNextPage).toBe(true);

        await act(() => result.current.fetchNextPage());
        await waitFor(() => expect(result.current.submissions).toHaveLength(12));
        expect(result.current.hasNextPage).toBe(false);

        const ids = result.current.submissions.map((submission) => submission.id);
        expect(new Set(ids).size).toBe(12);
    });

    it("trả danh sách rỗng cho người mới", async () => {
        const { result } = renderRecentSubmissions("newbie");

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.submissions).toEqual([]);
        expect(result.current.hasNextPage).toBe(false);
    });

    it("báo lỗi máy chủ kèm traceId", async () => {
        const { result } = renderRecentSubmissions("error");

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 500, traceId: "4bf92f3577b34da6" });
    });
});
