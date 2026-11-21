import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
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
    it("lấy số bài và khoá chờ duyệt từ hàng chờ của từng service", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderNavigationCounts();

        await waitFor(() => expect(result.current.data).toBeDefined());
        expect(result.current.data).toEqual({
            abuseReports: 12,
            supportMessages: 3,
            problemReviews: 1,
            courseReviews: 1,
            hasLiveContest: true,
            hasJudgeIncident: true,
        });
    });

    it("vẫn có số chờ duyệt khi chưa có service tổng hợp các số đếm khác", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        server.use(
            http.get("*/api/admin/navigation-counts", () =>
                HttpResponse.json({ status: 404, code: "NOT_FOUND" }, { status: 404 }),
            ),
        );

        const { result } = renderNavigationCounts();

        await waitFor(() => expect(result.current.data).toBeDefined());
        expect(result.current.data).toEqual({
            abuseReports: 0,
            supportMessages: 0,
            problemReviews: 1,
            courseReviews: 1,
            hasLiveContest: false,
            hasJudgeIncident: false,
        });
    });

    it("không hiện số nào khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        const { result } = renderNavigationCounts();

        await waitFor(() => expect(result.current.data).toBeDefined());
        expect(result.current.data).toMatchObject({
            abuseReports: 0,
            problemReviews: 0,
            courseReviews: 0,
            hasJudgeIncident: false,
        });
    });
});
