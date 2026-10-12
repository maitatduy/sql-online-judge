import { screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { renderWithProviders } from "@/test/render";

import { AuthShell } from "./AuthShell";

vi.mock("next/navigation", () => ({
    useRouter: () => ({ push: vi.fn() }),
}));

describe("AuthShell", () => {
    it("có tiêu đề chính, logo và link về trang chủ", () => {
        renderWithProviders(<AuthShell mode="login" nextPath="/" />);

        expect(
            screen.getByRole("heading", { level: 1, name: "Viết. Chạy. Accepted." }),
        ).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
        expect(screen.getByRole("link", { name: "SQL Online Judge" })).toHaveAttribute("href", "/");
    });

    it("chế độ đăng nhập hiện lời chào trở lại và mời đăng ký", () => {
        renderWithProviders(<AuthShell mode="login" nextPath="/" />);

        expect(
            screen.getByRole("heading", { level: 2, name: "Chào mừng trở lại" }),
        ).toBeInTheDocument();
        expect(screen.getByText("-- Chào mừng trở lại")).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Đăng ký miễn phí" })).toHaveAttribute(
            "href",
            "/register",
        );
    });

    it("chế độ đăng ký hiện lời mời tạo tài khoản và lối về đăng nhập", () => {
        renderWithProviders(<AuthShell mode="register" nextPath="/" />);

        expect(
            screen.getByRole("heading", { level: 2, name: "Tạo tài khoản miễn phí" }),
        ).toBeInTheDocument();
        expect(screen.getByText("-- Tạo tài khoản mới")).toBeInTheDocument();
        expect(screen.getByText("Đã có tài khoản?")).toBeInTheDocument();
    });

    it.each([
        ["login", "Đăng nhập", "Đăng ký"],
        ["register", "Đăng ký", "Đăng nhập"],
    ] as const)("ở chế độ %s, tab %s được đánh dấu đang mở", (mode, activeTab, otherTab) => {
        renderWithProviders(<AuthShell mode={mode} nextPath="/" />);

        const tabs = screen.getByRole("navigation", { name: "Chọn hình thức" });

        expect(within(tabs).getByRole("link", { name: activeTab })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(within(tabs).getByRole("link", { name: otherTab })).not.toHaveAttribute(
            "aria-current",
        );
        expect(within(tabs).getByRole("link", { name: "Đăng nhập" })).toHaveAttribute(
            "href",
            "/login",
        );
        expect(within(tabs).getByRole("link", { name: "Đăng ký" })).toHaveAttribute(
            "href",
            "/register",
        );
    });

    it("cột giới thiệu không có danh sách ý nổi bật", () => {
        renderWithProviders(<AuthShell mode="login" nextPath="/" />);

        const intro = screen.getByRole("region", { name: "Giới thiệu" });

        expect(within(intro).queryByRole("list")).not.toBeInTheDocument();
        expect(screen.queryByText("Chấm trong vài giây")).not.toBeInTheDocument();
    });

    it("có hai nút đăng nhập bằng GitHub và Google bấm được", () => {
        renderWithProviders(<AuthShell mode="login" nextPath="/" />);

        expect(screen.getByRole("button", { name: "Tiếp tục với GitHub" })).toBeEnabled();
        expect(screen.getByRole("button", { name: "Tiếp tục với Google" })).toBeEnabled();
    });

    it("ẩn hai nút GitHub, Google và dòng phân cách ở production khi chưa bật cờ", () => {
        vi.stubEnv("NODE_ENV", "production");

        renderWithProviders(<AuthShell mode="login" nextPath="/" />);

        expect(
            screen.queryByRole("button", { name: "Tiếp tục với GitHub" }),
        ).not.toBeInTheDocument();
        expect(screen.queryByText("hoặc dùng email")).not.toBeInTheDocument();
        vi.unstubAllEnvs();
    });

    it("đặt form vào vùng nội dung chính", () => {
        renderWithProviders(
            <AuthShell mode="login" nextPath="/">
                <p>Form đăng nhập</p>
            </AuthShell>,
        );

        expect(within(screen.getByRole("main")).getByText("Form đăng nhập")).toBeInTheDocument();
        expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
    });
});
