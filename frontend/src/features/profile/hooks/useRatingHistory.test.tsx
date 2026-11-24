import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { BROKEN_PROFILE_USER_ID, MINH_ANH_PROFILE, NEWBIE_PROFILE } from "@/mocks/profile-fixtures";

import { useRatingHistory } from "./useRatingHistory";

function renderRatingHistory(userId: string) {
    const queryClient = new QueryClient();
    return renderHook(() => useRatingHistory(userId), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useRatingHistory", () => {
    it("trả lịch sử rating theo thứ tự thời gian", async () => {
        const { result } = renderRatingHistory(MINH_ANH_PROFILE.profile.userId);

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        const points = result.current.data ?? [];
        expect(points).toHaveLength(8);
        expect(points[0]).toMatchObject({ rating: 1320, rank: 29 });
        expect(points[7]).toMatchObject({ rating: 1487, rank: 36 });
    });

    it("trả danh sách rỗng cho người chưa thi", async () => {
        const { result } = renderRatingHistory(NEWBIE_PROFILE.profile.userId);

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toEqual([]);
    });

    it("báo lỗi máy chủ kèm traceId", async () => {
        const { result } = renderRatingHistory(BROKEN_PROFILE_USER_ID);

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 500, traceId: "4bf92f3577b34da6" });
    });
});
