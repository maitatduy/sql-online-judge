import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useUserCounts } from "./useUserCounts";

function renderCounts() {
    const queryClient = new QueryClient();
    return renderHook(() => useUserCounts(), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useUserCounts", () => {
    it("trả về số tài khoản theo từng nhóm", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderCounts();

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toEqual({
            total: 62,
            unverified: 1,
            setters: 3,
            moderators: 2,
            banned: 2,
        });
    });

    it("báo 403 ngay, không thử lại, khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        const { result } = renderCounts();

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 403, code: "ACCESS_DENIED" });
        expect(result.current.failureCount).toBe(1);
    });
});
