import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useInviteUsers } from "./useInviteUsers";

describe("useInviteUsers", () => {
    it("gửi lời mời và trả số lời mời đã gửi", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const queryClient = new QueryClient();
        const { result } = renderHook(() => useInviteUsers(), {
            wrapper: ({ children }: { children: React.ReactNode }) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        });

        await act(async () => {
            await expect(
                result.current.mutateAsync({ emails: ["a@example.com"], role: "SETTER" }),
            ).resolves.toEqual({ invitedCount: 1 });
        });
    });
});
