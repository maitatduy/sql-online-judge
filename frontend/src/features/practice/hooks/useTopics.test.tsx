import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { useTopics } from "./useTopics";

describe("useTopics", () => {
    it("tải danh sách chủ đề", async () => {
        const queryClient = new QueryClient();
        const { result } = renderHook(() => useTopics(), {
            wrapper: ({ children }: { children: React.ReactNode }) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data?.items[0]).toEqual({
            slug: "select-where",
            name: "SELECT/WHERE",
        });
    });
});
