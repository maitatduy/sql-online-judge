import { fireEvent, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { navigateBrowserTo } from "@/lib/browser-navigation";
import { renderWithProviders } from "@/test/render";

import { readOAuthNextPath } from "../oauth-redirect";

import { OAuthButtons } from "./OAuthButtons";

vi.mock("@/lib/browser-navigation", () => ({ navigateBrowserTo: vi.fn() }));

const assign = vi.mocked(navigateBrowserTo);

describe("OAuthButtons", () => {
    beforeEach(() => {
        sessionStorage.clear();
        assign.mockClear();
    });

    it("hai nút đều bấm được", () => {
        renderWithProviders(<OAuthButtons nextPath="/" />);

        expect(screen.getByRole("button", { name: "Tiếp tục với GitHub" })).toBeEnabled();
        expect(screen.getByRole("button", { name: "Tiếp tục với Google" })).toBeEnabled();
    });

    it.each([
        ["GitHub", "github"],
        ["Google", "google"],
    ])("bấm %s thì chuyển hẳn trình duyệt sang trang uỷ quyền của máy chủ", (label, provider) => {
        renderWithProviders(<OAuthButtons nextPath="/problems" />);

        fireEvent.click(screen.getByRole("button", { name: `Tiếp tục với ${label}` }));

        expect(assign).toHaveBeenCalledWith(
            `http://localhost:3000/api/auth/oauth2/authorization/${provider}`,
        );
        expect(readOAuthNextPath()).toBe("/problems");
    });

    it("khoá cả hai nút sau khi bấm để không mở hai luồng", () => {
        renderWithProviders(<OAuthButtons nextPath="/" />);

        fireEvent.click(screen.getByRole("button", { name: "Tiếp tục với GitHub" }));

        expect(screen.getByRole("button", { name: "Đang kết nối với GitHub…" })).toBeDisabled();
        expect(screen.getByRole("button", { name: "Tiếp tục với Google" })).toBeDisabled();
    });
});
