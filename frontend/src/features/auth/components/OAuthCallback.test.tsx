import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { clearAccessToken, getAccessToken } from "@/lib/auth-session";
import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { saveOAuthNextPath } from "../oauth-redirect";

import { OAuthCallback } from "./OAuthCallback";

const replace = vi.fn();

vi.mock("next/navigation", () => ({
    useRouter: () => ({ replace }),
}));

describe("OAuthCallback", () => {
    beforeEach(() => {
        replace.mockClear();
        clearAccessToken();
        sessionStorage.clear();
    });

    it("đổi cookie lấy access token rồi về đúng trang trước khi đăng nhập", async () => {
        saveOAuthNextPath("/problems");
        server.use(
            http.post("*/api/auth/refresh", () =>
                HttpResponse.json({ accessToken: "oauth-access-token", user: {} }),
            ),
        );

        renderWithProviders(<OAuthCallback />);

        expect(screen.getByRole("status")).toHaveTextContent("Đang hoàn tất đăng nhập");
        expect(screen.getByTestId("auth-status-icon")).toHaveClass("motion-safe:animate-spin");
        await waitFor(() => expect(replace).toHaveBeenCalledWith("/problems"));
        expect(getAccessToken()).toBe("oauth-access-token");
    });

    it("không có trang đích thì về trang chủ; trang đích không an toàn thì bỏ qua", async () => {
        saveOAuthNextPath("https://evil.example");
        server.use(
            http.post("*/api/auth/refresh", () =>
                HttpResponse.json({ accessToken: "oauth-access-token", user: {} }),
            ),
        );

        renderWithProviders(<OAuthCallback />);

        await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
    });

    it("không đổi được cookie thì báo lỗi và mời đăng nhập lại", async () => {
        server.use(
            http.post("*/api/auth/refresh", () =>
                HttpResponse.json(
                    { status: 401, code: "INVALID_REFRESH_TOKEN", message: "Phiên đã hết hạn" },
                    { status: 401 },
                ),
            ),
        );

        renderWithProviders(<OAuthCallback />);

        expect(
            await screen.findByRole("heading", { level: 1, name: "Chưa đăng nhập được" }),
        ).toBeInTheDocument();
        expect(screen.getByRole("link", { name: "Quay lại đăng nhập" })).toHaveAttribute(
            "href",
            "/login",
        );
        expect(replace).not.toHaveBeenCalled();
    });
});
