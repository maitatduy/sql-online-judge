import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useCurrentUser } from "./useCurrentUser";

function renderCurrentUser() {
    const queryClient = new QueryClient();
    return renderHook(() => useCurrentUser(), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useCurrentUser", () => {
    it("trả về người dùng đang đăng nhập", async () => {
        writeMockSession(DEMO_SESSION_USER);

        const { result } = renderCurrentUser();

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toEqual({
            id: "0192f3a0-0000-7000-8000-000000000001",
            username: "minh.anh",
            displayName: "Nguyễn Minh Anh",
            role: "LEARNER",
            currentStreakDays: 12,
        });
    });

    it("báo lỗi 401 ngay, không thử lại, khi chưa đăng nhập", async () => {
        let requestCount = 0;
        server.use(
            http.get("*/api/auth/me", () => {
                requestCount += 1;
                return HttpResponse.json(
                    { status: 401, code: "UNAUTHENTICATED", message: "Bạn chưa đăng nhập" },
                    { status: 401 },
                );
            }),
        );

        const { result } = renderCurrentUser();

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 401, code: "UNAUTHENTICATED" });
        expect(requestCount).toBe(1);
    });
});
