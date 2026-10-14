import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useAdminUser } from "./useAdminUser";
import { useChangeUserRole } from "./useChangeUserRole";
import { useUserCounts } from "./useUserCounts";

function renderRoleChange() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderHook(
        () => ({
            user: useAdminUser("viet.luu"),
            counts: useUserCounts(),
            changeRole: useChangeUserRole(),
        }),
        {
            wrapper: ({ children }: { children: React.ReactNode }) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        },
    );
}

describe("useChangeUserRole", () => {
    it("cập nhật chi tiết ngay và làm mới số liệu sau khi đổi vai trò", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const { result } = renderRoleChange();
        await waitFor(() => expect(result.current.counts.data?.setters).toBe(3));

        await act(() =>
            result.current.changeRole.mutateAsync({ username: "viet.luu", role: "SETTER" }),
        );

        expect(result.current.user.data?.role).toBe("SETTER");
        await waitFor(() => expect(result.current.counts.data?.setters).toBe(4));
    });

    it("báo lỗi 422 khi tự đổi vai trò của mình", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const { result } = renderRoleChange();

        await act(async () => {
            await expect(
                result.current.changeRole.mutateAsync({ username: "admin.huy", role: "LEARNER" }),
            ).rejects.toMatchObject({ status: 422, code: "CANNOT_MODIFY_SELF" });
        });
    });
});
