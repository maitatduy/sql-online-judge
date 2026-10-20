import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { stubDownloads } from "@/test/downloads";

import { useExportUsers } from "./useExportUsers";

function renderExportHook() {
    const queryClient = new QueryClient();
    return renderHook(() => useExportUsers(), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useExportUsers", () => {
    it("tải file CSV nhận được về máy với tên file của máy chủ", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const downloads = stubDownloads();
        const { result } = renderExportHook();

        await act(async () => {
            await result.current.mutateAsync({ scope: "ALL", includeEmail: false });
        });

        expect(downloads.fileNames).toEqual([expect.stringMatching(/^users-\d{8}-\d{4}\.csv$/)]);
        expect(downloads.created).toHaveLength(1);
    });

    it("đặt tên users.csv khi máy chủ không gửi tên file", async () => {
        server.use(
            http.post("*/api/admin/user-exports", () =>
                HttpResponse.text("username\n", { headers: { "Content-Type": "text/csv" } }),
            ),
        );
        const downloads = stubDownloads();
        const { result } = renderExportHook();

        await act(async () => {
            await result.current.mutateAsync({ scope: "ALL", includeEmail: false });
        });

        expect(downloads.fileNames).toEqual(["users.csv"]);
    });

    it("không tải gì khi máy chủ báo lỗi", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const downloads = stubDownloads();
        const { result } = renderExportHook();

        await act(async () => {
            await expect(
                result.current.mutateAsync({ scope: "ALL", includeEmail: true }),
            ).rejects.toMatchObject({ status: 400, code: "VALIDATION_FAILED" });
        });

        expect(downloads.created).toHaveLength(0);
    });
});
