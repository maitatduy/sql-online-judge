import { fireEvent, screen, waitFor } from "@testing-library/react";
import { delay, http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { RegisterForm } from "./RegisterForm";

const push = vi.fn();

vi.mock("next/navigation", () => ({
    useRouter: () => ({ push }),
}));

const TERMS_LABEL = "Tôi đồng ý với Điều khoản sử dụng và Chính sách quyền riêng tư";

interface RegisterInput {
    username?: string;
    email?: string;
    password?: string;
    acceptTerms?: boolean;
}

function fillAndSubmit({
    username = "minh.anh",
    email = "minh.anh@sqljudge.dev",
    password = "mock-password",
    acceptTerms = true,
}: RegisterInput = {}) {
    fireEvent.change(screen.getByLabelText("Tên người dùng"), { target: { value: username } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: email } });
    fireEvent.change(screen.getByLabelText("Mật khẩu"), { target: { value: password } });
    if (acceptTerms) {
        fireEvent.click(screen.getByLabelText(TERMS_LABEL));
    }
    fireEvent.click(screen.getByRole("button", { name: "Tạo tài khoản" }));
}

describe("RegisterForm", () => {
    beforeEach(() => {
        push.mockClear();
    });

    it("báo lỗi cạnh từng ô và không gửi request khi để trống", async () => {
        const onRequest = vi.fn();
        server.use(
            http.post("*/api/auth/register", () => {
                onRequest();
                return HttpResponse.json({}, { status: 201 });
            }),
        );
        renderWithProviders(<RegisterForm nextPath="/" />);

        fireEvent.click(screen.getByRole("button", { name: "Tạo tài khoản" }));

        expect(await screen.findByText("Vui lòng nhập tên người dùng")).toBeInTheDocument();
        expect(screen.getByText("Vui lòng nhập email")).toBeInTheDocument();
        expect(screen.getByText("Vui lòng nhập mật khẩu")).toBeInTheDocument();
        expect(screen.getByText("Bạn cần đồng ý với điều khoản để tiếp tục")).toBeInTheDocument();
        expect(onRequest).not.toHaveBeenCalled();
    });

    it.each(["Minh Anh", "ab", "minh-anh", "MINH"])(
        "từ chối tên người dùng không hợp lệ: %s",
        async (username) => {
            renderWithProviders(<RegisterForm nextPath="/" />);

            fillAndSubmit({ username });

            expect(
                await screen.findByText(
                    "Tên người dùng gồm 3-30 ký tự: chữ thường, số, dấu chấm hoặc gạch dưới",
                ),
            ).toBeInTheDocument();
            expect(push).not.toHaveBeenCalled();
        },
    );

    it("từ chối mật khẩu dưới 8 ký tự", async () => {
        renderWithProviders(<RegisterForm nextPath="/" />);

        fillAndSubmit({ password: "abc123" });

        expect(await screen.findByText("Mật khẩu cần ít nhất 8 ký tự")).toBeInTheDocument();
    });

    it("yêu cầu đồng ý điều khoản", async () => {
        renderWithProviders(<RegisterForm nextPath="/" />);

        fillAndSubmit({ acceptTerms: false });

        expect(
            await screen.findByText("Bạn cần đồng ý với điều khoản để tiếp tục"),
        ).toBeInTheDocument();
        expect(push).not.toHaveBeenCalled();
    });

    it("đăng ký hợp lệ thì gửi đúng dữ liệu và chuyển trang", async () => {
        let receivedBody: unknown;
        server.use(
            http.post("*/api/auth/register", async ({ request }) => {
                receivedBody = await request.json();
                return HttpResponse.json({ accessToken: "t", user: {} }, { status: 201 });
            }),
        );
        renderWithProviders(<RegisterForm nextPath="/problems" />);

        fillAndSubmit();

        await waitFor(() => expect(push).toHaveBeenCalledWith("/problems"));
        expect(receivedBody).toEqual({
            username: "minh.anh",
            email: "minh.anh@sqljudge.dev",
            password: "mock-password",
            acceptedTerms: true,
        });
    });

    it("tên người dùng đã có thì báo lỗi ở ô tên người dùng", async () => {
        renderWithProviders(<RegisterForm nextPath="/" />);

        fillAndSubmit({ username: "taken" });

        expect(await screen.findByText("Tên người dùng này đã có người dùng")).toBeInTheDocument();
        expect(screen.getByLabelText("Tên người dùng")).toHaveAttribute("aria-invalid", "true");
        expect(push).not.toHaveBeenCalled();
    });

    it("email đã đăng ký thì báo lỗi ở ô email", async () => {
        renderWithProviders(<RegisterForm nextPath="/" />);

        fillAndSubmit({ email: "taken@sqljudge.dev" });

        expect(await screen.findByText("Email này đã được đăng ký")).toBeInTheDocument();
        expect(screen.getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
    });

    it("thanh độ mạnh cập nhật khi gõ mật khẩu", () => {
        renderWithProviders(<RegisterForm nextPath="/" />);
        const meter = screen.getByRole("meter", { name: "Độ mạnh mật khẩu" });

        expect(meter).toHaveAttribute("aria-valuenow", "0");

        fireEvent.change(screen.getByLabelText("Mật khẩu"), { target: { value: "abcdefg1" } });

        expect(meter).toHaveAttribute("aria-valuenow", "2");
        expect(meter).toHaveAttribute("aria-valuemax", "4");
    });

    it("nói trước độ dài tối thiểu của mật khẩu", () => {
        renderWithProviders(<RegisterForm nextPath="/" />);

        expect(screen.getByText("Ít nhất 8 ký tự")).toBeInTheDocument();
    });

    it("không liệt kê các tiêu chí mật khẩu", () => {
        renderWithProviders(<RegisterForm nextPath="/" />);

        expect(screen.queryByText("Từ 8 ký tự")).not.toBeInTheDocument();
        expect(screen.queryByText("Có chữ và số")).not.toBeInTheDocument();
        expect(screen.queryByText("Có ký tự đặc biệt")).not.toBeInTheDocument();
    });

    it("mất kết nối thì báo lỗi chung và cho gửi lại", async () => {
        server.use(http.post("*/api/auth/register", () => HttpResponse.error()));
        renderWithProviders(<RegisterForm nextPath="/" />);

        fillAndSubmit();

        expect(await screen.findByRole("alert")).toHaveTextContent("Không kết nối được máy chủ");
        expect(screen.getByRole("button", { name: "Tạo tài khoản" })).toBeEnabled();
    });

    it("khoá nút trong lúc đang gửi", async () => {
        server.use(
            http.post("*/api/auth/register", async () => {
                await delay(200);
                return HttpResponse.json({ accessToken: "t", user: {} }, { status: 201 });
            }),
        );
        renderWithProviders(<RegisterForm nextPath="/" />);

        fillAndSubmit();

        expect(await screen.findByRole("button", { name: "Đang tạo tài khoản…" })).toBeDisabled();
        await waitFor(() => expect(push).toHaveBeenCalled());
    });
});
