import { act, fireEvent, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { ForgotPasswordFlow } from "./ForgotPasswordFlow";

const EMAIL = "minh.anh@gmail.com";

function submitEmail(email: string) {
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: email } });
    fireEvent.click(screen.getByRole("button", { name: "Gửi liên kết đặt lại" }));
}

describe("ForgotPasswordFlow", () => {
    afterEach(() => {
        vi.useRealTimers();
    });

    it("gửi email rồi báo kiểm tra hộp thư mà không khẳng định email có tài khoản", async () => {
        let receivedBody: unknown;
        server.use(
            http.post("*/api/auth/password-reset", async ({ request }) => {
                receivedBody = await request.json();
                return new HttpResponse(null, { status: 202 });
            }),
        );
        renderWithProviders(<ForgotPasswordFlow />);

        submitEmail(EMAIL);

        expect(
            await screen.findByRole("heading", { level: 1, name: "Kiểm tra hộp thư" }),
        ).toBeInTheDocument();
        expect(receivedBody).toEqual({ email: EMAIL });
        expect(screen.getByText(/có tài khoản trên SQL Judge/)).toBeInTheDocument();
        expect(screen.getByText(EMAIL)).toBeInTheDocument();
    });

    it("email sai định dạng thì báo lỗi và không gửi request", async () => {
        const onRequest = vi.fn();
        server.use(
            http.post("*/api/auth/password-reset", () => {
                onRequest();
                return new HttpResponse(null, { status: 202 });
            }),
        );
        renderWithProviders(<ForgotPasswordFlow />);

        submitEmail("khong-phai-email");

        expect(await screen.findByText("Email không hợp lệ")).toBeInTheDocument();
        expect(onRequest).not.toHaveBeenCalled();
    });

    it("cho dùng email khác bằng cách quay lại form", async () => {
        renderWithProviders(<ForgotPasswordFlow />);
        submitEmail(EMAIL);

        fireEvent.click(await screen.findByRole("button", { name: "Dùng email khác" }));

        expect(
            screen.getByRole("heading", { level: 1, name: "Quên mật khẩu?" }),
        ).toBeInTheDocument();
    });

    it("chỉ cho gửi lại email sau 60 giây", async () => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        let requestCount = 0;
        server.use(
            http.post("*/api/auth/password-reset", () => {
                requestCount += 1;
                return new HttpResponse(null, { status: 202 });
            }),
        );
        renderWithProviders(<ForgotPasswordFlow />);
        submitEmail(EMAIL);

        expect(await screen.findByRole("button", { name: /Gửi lại email sau/ })).toBeDisabled();
        act(() => {
            vi.advanceTimersByTime(60_000);
        });
        fireEvent.click(screen.getByRole("button", { name: "Gửi lại email" }));

        await waitFor(() => expect(requestCount).toBe(2));
    });

    it("có link quay lại đăng nhập", () => {
        renderWithProviders(<ForgotPasswordFlow />);

        expect(screen.getByRole("link", { name: "Đăng nhập" })).toHaveAttribute("href", "/login");
    });
});
