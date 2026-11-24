import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MINH_ANH_PROFILE } from "@/mocks/profile-fixtures";

import { useProfile, useRatingSummary, useSolvedSummary } from "./useProfile";

function renderWithClient<T>(hook: () => T) {
    const queryClient = new QueryClient();
    return renderHook(hook, {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useProfile", () => {
    it("trả về hồ sơ công khai, không kèm email", async () => {
        const { result } = renderWithClient(() => useProfile("minh.anh"));

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toMatchObject({
            username: "minh.anh",
            displayName: "Nguyễn Minh Anh",
            school: "ĐH Bách khoa Hà Nội",
        });
        expect(result.current.data).not.toHaveProperty("email");
    });

    it("trả hồ sơ trống cho người mới", async () => {
        const { result } = renderWithClient(() => useProfile("newbie"));

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data?.bio).toBeNull();
        expect(result.current.data?.websiteUrl).toBeNull();
    });

    it("báo USER_NOT_FOUND khi không có người dùng", async () => {
        const { result } = renderWithClient(() => useProfile("khong-co"));

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 404, code: "USER_NOT_FOUND" });
    });

    it("báo lỗi máy chủ kèm traceId", async () => {
        const { result } = renderWithClient(() => useProfile("error"));

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({
            status: 500,
            traceId: "4bf92f3577b34da6",
        });
    });
});

describe("useRatingSummary và useSolvedSummary", () => {
    it("chờ có userId rồi mới tải", () => {
        const { result } = renderWithClient(() => useRatingSummary(undefined));

        expect(result.current.fetchStatus).toBe("idle");
    });

    it("trả rating, hạng và số bài đã giải theo userId", async () => {
        const userId = MINH_ANH_PROFILE.profile.userId;
        const { result } = renderWithClient(() => ({
            rating: useRatingSummary(userId),
            solved: useSolvedSummary(userId),
        }));

        await waitFor(() => expect(result.current.solved.isSuccess).toBe(true));
        await waitFor(() => expect(result.current.rating.isSuccess).toBe(true));

        expect(result.current.rating.data).toMatchObject({ rating: 1487, rank: 2341 });
        expect(result.current.solved.data?.solvedCount).toBe(63);
    });
});
