import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useAdminNavigationCounts } from "./useAdminNavigationCounts";

function renderNavigationCounts() {
    const queryClient = new QueryClient();
    return renderHook(() => useAdminNavigationCounts(), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useAdminNavigationCounts", () => {
    it("trả về số việc đang chờ cho quản trị viên", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderNavigationCounts();

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toEqual({
            abuseReports: 12,
            supportMessages: 3,
            problemReviews: 5,
            hasLiveContest: true,
            hasJudgeIncident: true,
        });
    });

    it("báo 403 ngay, không thử lại, khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        const { result } = renderNavigationCounts();

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 403, code: "ACCESS_DENIED" });
        expect(result.current.failureCount).toBe(1);
    });
});
