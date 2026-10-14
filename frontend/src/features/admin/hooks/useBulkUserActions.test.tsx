import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useBulkBanUsers } from "./useBulkBanUsers";
import { useBulkChangeRole } from "./useBulkChangeRole";
import { useUserCounts } from "./useUserCounts";

function renderBulkActions() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderHook(
        () => ({ counts: useUserCounts(), ban: useBulkBanUsers(), role: useBulkChangeRole() }),
        {
            wrapper: ({ children }: { children: React.ReactNode }) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        },
    );
}

describe("useBulkBanUsers", () => {
    it("khoá nhiều tài khoản rồi làm mới số liệu", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const { result } = renderBulkActions();
        await waitFor(() => expect(result.current.counts.data?.banned).toBe(2));

        await act(async () => {
            await expect(
                result.current.ban.mutateAsync({
                    usernames: ["tam.nguyen", "datahust"],
                    duration: "DAYS_7",
                    reason: "Spam",
                    notifyByEmail: false,
                }),
            ).resolves.toEqual({ affectedCount: 2, skippedUsernames: [] });
        });

        await waitFor(() => expect(result.current.counts.data?.banned).toBe(4));
    });
});

describe("useBulkChangeRole", () => {
    it("đổi vai trò nhiều tài khoản rồi làm mới số liệu", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const { result } = renderBulkActions();
        await waitFor(() => expect(result.current.counts.data?.moderators).toBe(2));

        await act(() =>
            result.current.role.mutateAsync({
                usernames: ["tam.nguyen", "viet.luu"],
                role: "MODERATOR",
                reason: "Thêm người kiểm duyệt",
            }),
        );

        await waitFor(() => expect(result.current.counts.data?.moderators).toBe(4));
    });
});
