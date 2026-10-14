import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUserPage } from "./AdminUserPage";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

function serverError() {
    return HttpResponse.json(
        { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra", traceId: "7c2e" },
        { status: 500 },
    );
}

function renderPage(username: string) {
    visit(`/admin/users/${username}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUserPage username={username} />
        </ToastProvider>,
    );
}

describe("các khối trên trang chi tiết tải độc lập", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("một khối lỗi chỉ báo lỗi trong khối đó và tải lại được", async () => {
        server.use(
            http.get("*/api/admin/users/:username/sessions", () => serverError(), { once: true }),
        );
        renderPage("viet.luu");

        const sessions = within(await screen.findByRole("region", { name: "Phiên đăng nhập" }));
        const alert = await sessions.findByRole("alert");

        expect(alert).toHaveTextContent("Chưa tải được dữ liệu. Đã có lỗi xảy ra");
        expect(alert).toHaveTextContent("Mã lỗi: 7c2e");
        expect(
            await within(screen.getByRole("region", { name: "Contest gần đây" })).findByText(
                "SQL Weekly #37",
            ),
        ).toBeInTheDocument();
        expect(
            await within(screen.getByRole("region", { name: "Ghi chú nội bộ" })).findByText(
                "mod.thu · 12/09/2026",
            ),
        ).toBeInTheDocument();

        fireEvent.click(sessions.getByRole("button", { name: "Thử lại" }));

        expect(await sessions.findByText("Chrome · Windows · TP.HCM")).toBeInTheDocument();
    });

    it("báo đang tải riêng cho từng khối", async () => {
        renderPage("viet.luu");

        expect(
            await screen.findByRole("status", { name: "Đang tải contest gần đây" }),
        ).toBeInTheDocument();
        expect(
            screen.getByRole("status", { name: "Đang tải phiên đăng nhập" }),
        ).toBeInTheDocument();
    });
});
