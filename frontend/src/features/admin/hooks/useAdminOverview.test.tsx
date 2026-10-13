import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useAdminOverview } from "./useAdminOverview";

function renderOverview() {
    const queryClient = new QueryClient();
    return renderHook(() => useAdminOverview(), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useAdminOverview", () => {
    it("trả về số liệu tính từ trước đến giờ", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderOverview();

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toMatchObject({
            users: { value: 48260, changePercent: 2, changeNote: "so với tuần trước" },
            submissions: { value: 1284530, changePercent: 4, changeNote: "so với tuần trước" },
            aiMessages: { value: 3905110, changePercent: 5, changeNote: "so với tuần trước" },
            queue: { size: 146, averageWaitMs: 3800, isDegraded: true },
            submissionTotals: { allTime: 1284530, today: 9807, last7Days: 44307 },
            verdictDistribution: [
                { verdict: "AC", count: 552348, percent: 43 },
                { verdict: "WA", count: 449586, percent: 35 },
                { verdict: "OM", count: 64227, percent: 5 },
                { verdict: "TLE", count: 89917, percent: 7 },
                { verdict: "RE", count: 102762, percent: 8 },
                { verdict: "FS", count: 25690, percent: 2 },
            ],
        });
    });

    it("trả về số bài nộp từng tháng của năm nay, tới tháng hiện tại", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderOverview();

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        const monthly = result.current.data?.monthlySubmissions;

        expect(monthly?.year).toBeGreaterThanOrEqual(2026);
        expect(monthly?.months.length).toBeGreaterThanOrEqual(1);
        expect(monthly?.months.length).toBeLessThanOrEqual(12);
        expect(monthly?.months[0]).toEqual({ month: 1, count: 86420 });
    });

    it("báo 403 ngay, không thử lại, khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        const { result } = renderOverview();

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 403, code: "ACCESS_DENIED" });
        expect(result.current.failureCount).toBe(1);
    });
});
