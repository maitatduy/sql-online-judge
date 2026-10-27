import { act, fireEvent, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { clearAccessToken, getAccessToken } from "@/lib/auth-session";
import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { savePendingNextPath, savePendingVerificationEmail } from "../pending-verification";

import { VerifyEmailFlow } from "./VerifyEmailFlow";

const EMAIL = "minh.anh@gmail.com";
const VALID_CODE = "482913";

function submitCode(code: string) {
    fireEvent.change(screen.getByLabelText("Mã xác thực"), { target: { value: code } });
    fireEvent.click(screen.getByRole("button", { name: "Xác thực" }));
}

describe("VerifyEmailFlow", () => {
    beforeEach(() => {
        sessionStorage.clear();
        localStorage.clear();
        clearAccessToken();
        savePendingVerificationEmail(EMAIL);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it("cho biết mã đã gửi tới email vừa đăng ký", () => {
        renderWithProviders(<VerifyEmailFlow />);

        expect(
            screen.getByRole("heading", { level: 1, name: "Xác thực email của bạn" }),
        ).toBeInTheDocument();
        expect(screen.getByText(EMAIL)).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Đổi email" })).toHaveAttribute(
            "href",
            "/register",
        );
    });

    it("nhập đúng mã thì đăng nhập luôn và cho tiếp tục vào trang chính", async () => {
        let receivedBody: unknown;
        server.use(
            http.post("*/api/auth/email-verification/confirm", async ({ request }) => {
                receivedBody = await request.json();
                return HttpResponse.json({
                    accessToken: "fresh-access-token",
                    user: { id: "u1", username: "minh.anh", email: EMAIL, role: "LEARNER" },
                });
            }),
        );
        renderWithProviders(<VerifyEmailFlow />);

        submitCode(VALID_CODE);

        expect(
            await screen.findByRole("heading", { level: 1, name: "Email đã được xác thực" }),
        ).toBeInTheDocument();
        expect(receivedBody).toEqual({ email: EMAIL, code: VALID_CODE });
        expect(screen.getByText("minh.anh")).toBeInTheDocument();
        expect(getAccessToken()).toBe("fresh-access-token");
        expect(screen.getByRole("link", { name: "Tiếp tục" })).toHaveAttribute("href", "/");
    });

    it("mã không đủ 6 chữ số thì báo lỗi và không gửi request", async () => {
        const onRequest = vi.fn();
        server.use(
            http.post("*/api/auth/email-verification/confirm", () => {
                onRequest();
                return new HttpResponse(null, { status: 204 });
            }),
        );
        renderWithProviders(<VerifyEmailFlow />);

        submitCode("12");

        expect(await screen.findByText("Mã xác thực gồm 6 chữ số")).toBeInTheDocument();
        expect(onRequest).not.toHaveBeenCalled();
    });

    it("mã sai thì báo lỗi ở ô nhập mã và cho nhập lại", async () => {
        renderWithProviders(<VerifyEmailFlow />);

        submitCode("000000");

        expect(await screen.findByText("Mã không hợp lệ hoặc đã hết hạn")).toBeInTheDocument();
        expect(screen.getByLabelText("Mã xác thực")).toHaveAttribute("aria-invalid", "true");
        expect(screen.getByRole("button", { name: "Xác thực" })).toBeEnabled();
    });

    it("mở bằng link hợp lệ thì tự xác thực", async () => {
        renderWithProviders(<VerifyEmailFlow token="valid-link-token" />);

        expect(
            await screen.findByRole("heading", { level: 1, name: "Email đã được xác thực" }),
        ).toBeInTheDocument();
    });

    it("đăng ký từ lời mời thì nhập mã xong được đưa về trang lời mời", async () => {
        savePendingNextPath("/invitations/abc");
        renderWithProviders(<VerifyEmailFlow />);

        submitCode(VALID_CODE);

        expect(await screen.findByRole("link", { name: "Tiếp tục" })).toHaveAttribute(
            "href",
            "/invitations/abc",
        );
    });

    it("mở link xác thực ở tab khác vẫn được đưa về trang lời mời", async () => {
        sessionStorage.clear();
        savePendingNextPath("/invitations/abc");
        renderWithProviders(<VerifyEmailFlow token="valid-link-token" />);

        expect(await screen.findByRole("link", { name: "Tiếp tục" })).toHaveAttribute(
            "href",
            "/invitations/abc",
        );
    });

    it("đích quay lại không an toàn thì về trang chính", async () => {
        savePendingNextPath("//evil.example");
        renderWithProviders(<VerifyEmailFlow token="valid-link-token" />);

        expect(await screen.findByRole("link", { name: "Tiếp tục" })).toHaveAttribute("href", "/");
    });

    it("mở bằng link hết hạn thì báo lỗi và cho quay lại đăng nhập", async () => {
        renderWithProviders(<VerifyEmailFlow token="expired-link-token" />);

        expect(
            await screen.findByRole("heading", {
                level: 1,
                name: "Mã không hợp lệ hoặc đã hết hạn",
            }),
        ).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Quay lại đăng nhập" })).toHaveAttribute(
            "href",
            "/login",
        );
    });

    it("chỉ cho gửi lại mã sau 60 giây và đếm ngược lại sau khi gửi", async () => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        let resentTo: unknown;
        server.use(
            http.post("*/api/auth/email-verification", async ({ request }) => {
                resentTo = await request.json();
                return new HttpResponse(null, { status: 202 });
            }),
        );
        renderWithProviders(<VerifyEmailFlow />);

        expect(screen.getByRole("button", { name: /Gửi lại mã sau 01:00/ })).toBeDisabled();

        act(() => {
            vi.advanceTimersByTime(60_000);
        });
        fireEvent.click(screen.getByRole("button", { name: "Gửi lại mã" }));

        expect(await screen.findByText("Đã gửi mã mới tới email của bạn")).toBeInTheDocument();
        await waitFor(() => expect(resentTo).toEqual({ email: EMAIL }));
        expect(screen.getByRole("button", { name: /Gửi lại mã sau/ })).toBeDisabled();
    });

    it("không biết email đang chờ thì hướng dẫn bấm link trong email", () => {
        sessionStorage.clear();

        renderWithProviders(<VerifyEmailFlow />);

        expect(screen.getByText(/email bạn đã đăng ký/)).toBeInTheDocument();
        expect(screen.queryByLabelText("Mã xác thực")).not.toBeInTheDocument();
    });
});
