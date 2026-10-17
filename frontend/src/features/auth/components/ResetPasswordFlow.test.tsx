import { fireEvent, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { ResetPasswordFlow } from "./ResetPasswordFlow";

function submitPasswords(password: string, confirmation: string) {
    fireEvent.change(screen.getByLabelText("Mật khẩu mới"), { target: { value: password } });
    fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu mới"), {
        target: { value: confirmation },
    });
    fireEvent.click(screen.getByRole("button", { name: "Lưu mật khẩu mới" }));
}

describe("ResetPasswordFlow", () => {
    it("đặt mật khẩu mới thành công thì mời đăng nhập lại", async () => {
        let receivedBody: unknown;
        server.use(
            http.post("*/api/auth/password-reset/confirm", async ({ request }) => {
                receivedBody = await request.json();
                return new HttpResponse(null, { status: 204 });
            }),
        );
        renderWithProviders(<ResetPasswordFlow token="valid-reset-token" />);

        submitPasswords("matkhau-moi-2026", "matkhau-moi-2026");

        expect(
            await screen.findByRole("heading", { level: 1, name: "Đã đổi mật khẩu" }),
        ).toBeInTheDocument();
        expect(receivedBody).toEqual({
            token: "valid-reset-token",
            newPassword: "matkhau-moi-2026",
        });
        expect(screen.getByRole("link", { name: "Đăng nhập ngay" })).toHaveAttribute(
            "href",
            "/login",
        );
    });

    it("hai mật khẩu không khớp thì báo lỗi và không gửi request", async () => {
        const onRequest = vi.fn();
        server.use(
            http.post("*/api/auth/password-reset/confirm", () => {
                onRequest();
                return new HttpResponse(null, { status: 204 });
            }),
        );
        renderWithProviders(<ResetPasswordFlow token="valid-reset-token" />);

        submitPasswords("matkhau-moi-2026", "matkhau-moi-202");

        expect(await screen.findByText("Hai mật khẩu chưa khớp")).toBeInTheDocument();
        expect(onRequest).not.toHaveBeenCalled();
    });

    it("máy chủ chê mật khẩu thì báo lỗi ngay ở ô mật khẩu mới", async () => {
        server.use(
            http.post("*/api/auth/password-reset/confirm", () =>
                HttpResponse.json(
                    {
                        status: 400,
                        code: "VALIDATION_FAILED",
                        message: "Dữ liệu gửi lên không hợp lệ",
                        errors: [
                            {
                                field: "newPassword",
                                message: "Mật khẩu này quá phổ biến, hãy chọn mật khẩu khác",
                            },
                        ],
                    },
                    { status: 400 },
                ),
            ),
        );
        renderWithProviders(<ResetPasswordFlow token="valid-reset-token" />);

        submitPasswords("password123", "password123");

        expect(
            await screen.findByText("Mật khẩu này quá phổ biến, hãy chọn mật khẩu khác"),
        ).toBeInTheDocument();
        expect(screen.getByLabelText("Mật khẩu mới")).toHaveAttribute("aria-invalid", "true");
    });

    it("liên kết hết hạn thì mời gửi liên kết mới", async () => {
        renderWithProviders(<ResetPasswordFlow token="expired-reset-token" />);

        submitPasswords("matkhau-moi-2026", "matkhau-moi-2026");

        expect(
            await screen.findByRole("heading", { level: 1, name: "Liên kết đã hết hạn" }),
        ).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Gửi liên kết mới" })).toHaveAttribute(
            "href",
            "/forgot-password",
        );
    });

    it("mở trang không có liên kết thì báo hết hạn ngay", () => {
        renderWithProviders(<ResetPasswordFlow />);

        expect(
            screen.getByRole("heading", { level: 1, name: "Liên kết đã hết hạn" }),
        ).toBeInTheDocument();
    });
});
