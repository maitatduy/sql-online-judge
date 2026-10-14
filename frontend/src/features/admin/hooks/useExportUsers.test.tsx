import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useExportUsers } from "./useExportUsers";

describe("useExportUsers", () => {
    it("gửi yêu cầu xuất và trả mã yêu cầu", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const queryClient = new QueryClient();
        const { result } = renderHook(() => useExportUsers(), {
            wrapper: ({ children }: { children: React.ReactNode }) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        });

        await act(async () => {
            await expect(
                result.current.mutateAsync({ scope: "ALL", includeEmail: false }),
            ).resolves.toMatchObject({ exportId: expect.stringMatching(/^export-/) });
        });
    });
});
