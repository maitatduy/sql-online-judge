import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";

import { useResendVerification } from "./useResendVerification";
import { useSendPasswordReset } from "./useSendPasswordReset";

function renderEmailActions() {
    const queryClient = new QueryClient();
    return renderHook(
        () => ({ sendReset: useSendPasswordReset(), resend: useResendVerification() }),
        {
            wrapper: ({ children }: { children: React.ReactNode }) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        },
    );
}

describe("useSendPasswordReset", () => {
    it("gửi yêu cầu đặt lại mật khẩu cho tài khoản", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const { result } = renderEmailActions();

        await act(async () => {
            await expect(result.current.sendReset.mutateAsync("viet.luu")).resolves.toEqual({
                username: "viet.luu",
            });
        });
    });
});

describe("useResendVerification", () => {
    it("gửi lại email xác thực cho tài khoản chưa xác minh", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const { result } = renderEmailActions();

        await act(async () => {
            await expect(result.current.resend.mutateAsync("tam.nguyen")).resolves.toEqual({
                username: "tam.nguyen",
            });
        });
    });

    it("báo 409 khi email đã được xác minh", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        const { result } = renderEmailActions();

        await act(async () => {
            await expect(result.current.resend.mutateAsync("viet.luu")).rejects.toMatchObject({
                status: 409,
                code: "EMAIL_ALREADY_VERIFIED",
            });
        });
    });
});
