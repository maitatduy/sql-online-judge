import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";

import { NO_USERS_FILTERS, type UsersFilters } from "../users/search-params";
import { useAdminUsers } from "./useAdminUsers";

function renderUsers(change: Partial<UsersFilters>) {
    const filters = { ...NO_USERS_FILTERS, ...change };
    const queryClient = new QueryClient();
    return renderHook(() => useAdminUsers(filters), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useAdminUsers", () => {
    it("trả về trang người dùng được yêu cầu", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderUsers({ page: 4 });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toMatchObject({ page: 4, size: 20, totalItems: 62 });
        expect(result.current.data?.items).toHaveLength(2);
    });

    it.each([
        ["ACTIVE", 59],
        ["BANNED", 2],
        ["UNVERIFIED", 1],
    ] as const)("lọc trạng thái %s ra %d tài khoản", async (state, expectedCount) => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderUsers({ state });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data?.totalItems).toBe(expectedCount);
    });

    it("gửi từ khoá và vai trò lên máy chủ", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderUsers({ q: "thu", role: "MODERATOR" });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data?.items.map((user) => user.username)).toEqual(["mod.thu"]);
    });

    it("gửi cách sắp xếp lên máy chủ", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderUsers({ sort: { field: "acceptedCount", direction: "desc" } });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data?.items[2].username).toBe("mod.thu");
    });

    it("báo 403 ngay, không thử lại, khi không phải quản trị viên", async () => {
        writeMockSession(DEMO_SESSION_USER);

        const { result } = renderUsers({});

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 403, code: "ACCESS_DENIED" });
        expect(result.current.failureCount).toBe(1);
    });
});
