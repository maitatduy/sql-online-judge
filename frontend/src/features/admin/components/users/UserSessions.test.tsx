import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { apiClient } from "@/lib/api-client";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUserPage } from "./AdminUserPage";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

function renderPage(username: string, search = "") {
    visit(`/admin/users/${username}${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUserPage username={username} />
        </ToastProvider>,
    );
}

async function findSessions() {
    return within(await screen.findByRole("region", { name: "Phiên đăng nhập" }));
}

describe("phiên đăng nhập trên trang chi tiết", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("tổng quan hiện các phiên kèm nhãn IP lạ, không hiện địa chỉ IP", async () => {
        renderPage("viet.luu");

        const sessions = await findSessions();

        expect(await sessions.findByText("Chrome · Windows · TP.HCM")).toBeInTheDocument();
        expect(sessions.getByText("Đang hoạt động")).toBeInTheDocument();
        expect(sessions.getByText("IP lạ")).toBeInTheDocument();
        expect(sessions.queryByText(/\d+\.\d+\.\d+\.\d+/)).not.toBeInTheDocument();
    });

    it("đăng xuất từng phiên ở tab Phiên đăng nhập", async () => {
        renderPage("viet.luu", "?tab=sessions");
        const sessions = await findSessions();

        fireEvent.click(
            await sessions.findByRole("button", {
                name: "Đăng xuất phiên Safari · iPhone · Hà Nội",
            }),
        );

        expect(
            await screen.findByText("Đã đăng xuất phiên Safari · iPhone · Hà Nội."),
        ).toBeInTheDocument();
        await waitFor(() =>
            expect(sessions.queryByText("Safari · iPhone · Hà Nội")).not.toBeInTheDocument(),
        );
    });

    it("đăng xuất mọi thiết bị sau khi xác nhận", async () => {
        renderPage("viet.luu", "?tab=sessions");
        const sessions = await findSessions();

        fireEvent.click(await sessions.findByRole("button", { name: "Đăng xuất mọi thiết bị" }));
        const dialog = within(
            await screen.findByRole("dialog", { name: "Đăng xuất mọi thiết bị của viet.luu?" }),
        );

        expect(dialog.queryByText(/Bạn cũng sẽ bị đăng xuất/)).not.toBeInTheDocument();

        fireEvent.click(dialog.getByRole("button", { name: "Đăng xuất mọi thiết bị" }));

        expect(
            await screen.findByText("Đã đăng xuất mọi thiết bị của viet.luu."),
        ).toBeInTheDocument();
        expect(await sessions.findByText("Chưa có phiên đăng nhập nào.")).toBeInTheDocument();
    });

    it("cảnh báo khi đăng xuất mọi thiết bị của chính mình", async () => {
        renderPage("admin.huy", "?tab=sessions");
        const sessions = await findSessions();

        fireEvent.click(await sessions.findByRole("button", { name: "Đăng xuất mọi thiết bị" }));
        const dialog = await screen.findByRole("dialog", {
            name: "Đăng xuất mọi thiết bị của admin.huy?",
        });

        expect(dialog).toHaveTextContent("Bạn cũng sẽ bị đăng xuất khỏi phiên đang dùng.");
    });

    it("chỉ cho xem phiên của tài khoản đã xoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete("/api/admin/users/viet.luu", { reason: "Tạo nhầm" });
        renderPage("viet.luu", "?tab=sessions");
        const sessions = await findSessions();

        await sessions.findByText("Chrome · Windows · TP.HCM");

        expect(sessions.queryByRole("button", { name: /^Đăng xuất/ })).not.toBeInTheDocument();
    });
});
