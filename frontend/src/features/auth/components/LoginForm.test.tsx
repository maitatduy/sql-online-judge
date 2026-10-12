import { fireEvent, screen, waitFor } from "@testing-library/react";
import { delay, http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { LoginForm } from "./LoginForm";

const push = vi.fn();

vi.mock("next/navigation", () => ({
    useRouter: () => ({ push }),
}));

function fillAndSubmit(email: string, password: string) {
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: email } });
    fireEvent.change(screen.getByLabelText("Mật khẩu"), { target: { value: password } });
    fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));
}

describe("LoginForm", () => {
    beforeEach(() => {
        push.mockClear();
    });

    it("báo lỗi cạnh từng ô và không gửi request khi để trống", async () => {
        const onRequest = vi.fn();
        server.use(
            http.post("*/api/auth/login", () => {
                onRequest();
                return HttpResponse.json({});
            }),
        );
        renderWithProviders(<LoginForm nextPath="/" />);

        fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));

        expect(await screen.findByText("Vui lòng nhập email")).toBeInTheDocument();
        expect(screen.getByText("Vui lòng nhập mật khẩu")).toBeInTheDocument();
        expect(screen.getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
        expect(onRequest).not.toHaveBeenCalled();
    });

    it("báo email sai định dạng", async () => {
        renderWithProviders(<LoginForm nextPath="/" />);

        fillAndSubmit("khong-phai-email", "mock-password");

        expect(await screen.findByText("Email không hợp lệ")).toBeInTheDocument();
    });

    it("đăng nhập đúng thì chuyển tới đường dẫn kế tiếp", async () => {
        renderWithProviders(<LoginForm nextPath="/problems" />);

        fillAndSubmit("demo@sqljudge.dev", "mock-password");

        await waitFor(() => expect(push).toHaveBeenCalledWith("/problems"));
    });

    it("có link quên mật khẩu", () => {
        renderWithProviders(<LoginForm nextPath="/" />);

        expect(screen.getByRole("link", { name: "Quên mật khẩu?" })).toHaveAttribute(
            "href",
            "/forgot-password",
        );
    });

    it("gửi kèm lựa chọn ghi nhớ đăng nhập", async () => {
        let receivedBody: unknown;
        server.use(
            http.post("*/api/auth/login", async ({ request }) => {
                receivedBody = await request.json();
                return HttpResponse.json({ accessToken: "t", user: {} });
            }),
        );
        renderWithProviders(<LoginForm nextPath="/" />);

        fireEvent.click(screen.getByLabelText("Ghi nhớ đăng nhập trên thiết bị này"));
        fillAndSubmit("demo@sqljudge.dev", "mock-password");

        await waitFor(() => expect(push).toHaveBeenCalled());
        expect(receivedBody).toEqual({
            email: "demo@sqljudge.dev",
            password: "mock-password",
            rememberMe: true,
        });
    });

    it("sai thông tin thì báo lỗi chung và giữ nguyên email", async () => {
        renderWithProviders(<LoginForm nextPath="/" />);

        fillAndSubmit("ai-do@sqljudge.dev", "sai-mat-khau");

        expect(await screen.findByRole("alert")).toHaveTextContent(
            "Email hoặc mật khẩu không đúng",
        );
        expect(screen.getByLabelText("Email")).toHaveValue("ai-do@sqljudge.dev");
        expect(push).not.toHaveBeenCalled();
    });

    it("bị giới hạn thì báo số giây phải chờ", async () => {
        renderWithProviders(<LoginForm nextPath="/" />);

        fillAndSubmit("locked@sqljudge.dev", "mock-password");

        expect(await screen.findByRole("alert")).toHaveTextContent("thử lại sau 30 giây");
    });

    it("gắn lỗi theo trường từ API vào đúng ô", async () => {
        server.use(
            http.post("*/api/auth/login", () =>
                HttpResponse.json(
                    {
                        status: 400,
                        code: "VALIDATION_FAILED",
                        message: "Dữ liệu gửi lên không hợp lệ",
                        errors: [{ field: "email", message: "Email chưa được xác minh" }],
                    },
                    { status: 400 },
                ),
            ),
        );
        renderWithProviders(<LoginForm nextPath="/" />);

        fillAndSubmit("demo@sqljudge.dev", "mock-password");

        expect(await screen.findByText("Email chưa được xác minh")).toBeInTheDocument();
        expect(screen.getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
    });

    it("mất kết nối thì báo lỗi và cho gửi lại", async () => {
        server.use(http.post("*/api/auth/login", () => HttpResponse.error()));
        renderWithProviders(<LoginForm nextPath="/" />);

        fillAndSubmit("demo@sqljudge.dev", "mock-password");

        expect(await screen.findByRole("alert")).toHaveTextContent("Không kết nối được máy chủ");
        expect(screen.getByRole("button", { name: "Đăng nhập" })).toBeEnabled();
    });

    it("khoá nút trong lúc đang gửi", async () => {
        server.use(
            http.post("*/api/auth/login", async () => {
                await delay(200);
                return HttpResponse.json({ accessToken: "t", user: {} });
            }),
        );
        renderWithProviders(<LoginForm nextPath="/" />);

        fillAndSubmit("demo@sqljudge.dev", "mock-password");

        expect(await screen.findByRole("button", { name: "Đang đăng nhập…" })).toBeDisabled();
        await waitFor(() => expect(push).toHaveBeenCalled());
    });

    it("nút hiện mật khẩu đổi kiểu ô nhập", () => {
        renderWithProviders(<LoginForm nextPath="/" />);
        const passwordInput = screen.getByLabelText("Mật khẩu");

        expect(passwordInput).toHaveAttribute("type", "password");

        fireEvent.click(screen.getByRole("button", { name: "Hiện mật khẩu" }));

        expect(passwordInput).toHaveAttribute("type", "text");
        expect(screen.getByRole("button", { name: "Ẩn mật khẩu" })).toBeInTheDocument();
    });
});

describe("LoginForm - focus", () => {
    it("đưa focus về ô lỗi đầu tiên khi gửi form chưa hợp lệ", async () => {
        renderWithProviders(<LoginForm nextPath="/" />);

        fireEvent.click(screen.getByRole("button", { name: "Đăng nhập" }));

        await waitFor(() => expect(screen.getByLabelText("Email")).toHaveFocus());
    });
});
