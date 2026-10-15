import { act, fireEvent, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { savePendingVerificationEmail } from "../pending-verification";

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

    it("nhập đúng mã thì báo đã xác thực và dẫn tới đăng nhập", async () => {
        let receivedBody: unknown;
        server.use(
            http.post("*/api/auth/email-verification/confirm", async ({ request }) => {
                receivedBody = await request.json();
                return new HttpResponse(null, { status: 204 });
            }),
        );
        renderWithProviders(<VerifyEmailFlow />);

        submitCode(VALID_CODE);

        expect(
            await screen.findByRole("heading", { level: 1, name: "Email đã được xác thực" }),
        ).toBeInTheDocument();
        expect(receivedBody).toEqual({ email: EMAIL, code: VALID_CODE });
        expect(screen.getByRole("link", { name: "Đăng nhập để tiếp tục" })).toHaveAttribute(
            "href",
            "/login",
        );
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
