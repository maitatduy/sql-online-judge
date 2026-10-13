import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { useActivity } from "./useActivity";

function renderActivity(username: string) {
    const queryClient = new QueryClient();
    return renderHook(() => useActivity(username), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useActivity", () => {
    it("trả số bài nộp theo ngày, tổng khớp với từng ngày cộng lại", async () => {
        const { result } = renderActivity("minh.anh");

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        const activity = result.current.data;
        const summedCount = (activity?.days ?? []).reduce((sum, day) => sum + day.count, 0);

        expect(activity?.days.length).toBeGreaterThan(0);
        expect(activity?.days[0].date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
        expect(activity?.totalSubmissions).toBe(summedCount);
    });

    it("trả hoạt động rỗng cho người mới", async () => {
        const { result } = renderActivity("newbie");

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toEqual({ totalSubmissions: 0, days: [] });
    });

    it("báo lỗi máy chủ kèm traceId", async () => {
        const { result } = renderActivity("error");

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 500, traceId: "4bf92f3577b34da6" });
    });
});
