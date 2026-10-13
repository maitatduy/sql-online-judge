import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { useRatingHistory } from "./useRatingHistory";

function renderRatingHistory(username: string) {
    const queryClient = new QueryClient();
    return renderHook(() => useRatingHistory(username), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useRatingHistory", () => {
    it("trả lịch sử rating theo thứ tự thời gian", async () => {
        const { result } = renderRatingHistory("minh.anh");

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        const points = result.current.data ?? [];
        expect(points).toHaveLength(8);
        expect(points[0]).toMatchObject({ rating: 1320, rank: 29 });
        expect(points[7]).toMatchObject({ rating: 1487, rank: 36 });
    });

    it("trả danh sách rỗng cho người chưa thi", async () => {
        const { result } = renderRatingHistory("newbie");

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toEqual([]);
    });

    it("báo lỗi máy chủ kèm traceId", async () => {
        const { result } = renderRatingHistory("error");

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 500, traceId: "4bf92f3577b34da6" });
    });

    it("báo USER_NOT_FOUND khi không có người dùng", async () => {
        const { result } = renderRatingHistory("khong-co");

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 404, code: "USER_NOT_FOUND" });
    });
});
