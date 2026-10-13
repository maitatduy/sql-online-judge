import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { useProfile } from "./useProfile";

function renderProfile(username: string) {
    const queryClient = new QueryClient();
    return renderHook(() => useProfile(username), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useProfile", () => {
    it("trả về hồ sơ công khai, không kèm email", async () => {
        const { result } = renderProfile("minh.anh");

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toMatchObject({
            username: "minh.anh",
            displayName: "Nguyễn Minh Anh",
            rating: 1487,
            rankTitle: "Chuyên viên",
            followerCount: 128,
            followingCount: 41,
        });
        expect(result.current.data).not.toHaveProperty("email");
    });

    it("trả hồ sơ trống cho người mới", async () => {
        const { result } = renderProfile("newbie");

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data?.bio).toBeUndefined();
        expect(result.current.data?.badges).toEqual([]);
        expect(result.current.data?.stats.globalRank).toBeNull();
    });

    it("báo USER_NOT_FOUND khi không có người dùng", async () => {
        const { result } = renderProfile("khong-co");

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 404, code: "USER_NOT_FOUND" });
    });

    it("báo lỗi máy chủ kèm traceId", async () => {
        const { result } = renderProfile("error");

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({
            status: 500,
            traceId: "4bf92f3577b34da6",
        });
    });
});
