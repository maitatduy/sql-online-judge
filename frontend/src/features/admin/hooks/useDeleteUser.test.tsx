import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useDeleteUser } from "./useDeleteUser";
import { useRestoreUser } from "./useRestoreUser";
import { useUserCounts } from "./useUserCounts";

describe("useDeleteUser và useRestoreUser", () => {
    it("xoá rồi khôi phục tài khoản, làm mới số liệu sau mỗi lần", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        const { result } = renderHook(
            () => ({
                counts: useUserCounts(),
                deleteUser: useDeleteUser(),
                restoreUser: useRestoreUser(),
            }),
            {
                wrapper: ({ children }: { children: React.ReactNode }) => (
                    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
                ),
            },
        );
        await waitFor(() => expect(result.current.counts.data?.total).toBe(62));

        await act(() =>
            result.current.deleteUser.mutateAsync({ username: "viet.luu", reason: "Trùng" }),
        );

        await waitFor(() => expect(result.current.counts.data?.total).toBe(61));

        await act(() => result.current.restoreUser.mutateAsync("viet.luu"));

        await waitFor(() => expect(result.current.counts.data?.total).toBe(62));
    });
});
