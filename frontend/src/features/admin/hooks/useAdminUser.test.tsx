import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useAdminUser } from "./useAdminUser";

function renderUser(username: string) {
    const queryClient = new QueryClient();
    return renderHook(() => useAdminUser(username), {
        wrapper: ({ children }: { children: React.ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        ),
    });
}

describe("useAdminUser", () => {
    it("trả về chi tiết của tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderUser("copy.paste");

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(result.current.data).toMatchObject({
            username: "copy.paste",
            status: "BANNED",
            signupProvider: "GOOGLE",
        });
        expect(result.current.data?.violations).toHaveLength(1);
    });

    it("báo 404 ngay, không thử lại, khi không có tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);

        const { result } = renderUser("khong.co");

        await waitFor(() => expect(result.current.isError).toBe(true));

        expect(result.current.error).toMatchObject({ status: 404, code: "USER_NOT_FOUND" });
        expect(result.current.failureCount).toBe(1);
    });
});
