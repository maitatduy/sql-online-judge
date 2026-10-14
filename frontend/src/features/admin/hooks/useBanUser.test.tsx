import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useAdminUser } from "./useAdminUser";
import { useBanUser } from "./useBanUser";
import { useUnbanUser } from "./useUnbanUser";
import { useUserCounts } from "./useUserCounts";

function renderBanFlow(username: string) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderHook(
        () => ({
            user: useAdminUser(username),
            counts: useUserCounts(),
            ban: useBanUser(),
            unban: useUnbanUser(),
        }),
        {
            wrapper: ({ children }: { children: React.ReactNode }) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        },
    );
}

describe("useBanUser và useUnbanUser", () => {
    it("khoá thì cập nhật chi tiết ngay và làm mới số liệu", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const { result } = renderBanFlow("viet.luu");
        await waitFor(() => expect(result.current.counts.data?.banned).toBe(2));

        await act(() =>
            result.current.ban.mutateAsync({
                username: "viet.luu",
                duration: "PERMANENT",
                reason: "Tái phạm",
                notifyByEmail: true,
            }),
        );

        expect(result.current.user.data).toMatchObject({ status: "BANNED", bannedUntil: null });
        await waitFor(() => expect(result.current.counts.data?.banned).toBe(3));
    });

    it("mở khoá thì cập nhật chi tiết ngay và làm mới số liệu", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const { result } = renderBanFlow("copy.paste");
        await waitFor(() => expect(result.current.counts.data?.banned).toBe(2));

        await act(() =>
            result.current.unban.mutateAsync({ username: "copy.paste", reason: "Hết hạn" }),
        );

        expect(result.current.user.data?.status).toBe("ACTIVE");
        await waitFor(() => expect(result.current.counts.data?.banned).toBe(1));
    });
});
