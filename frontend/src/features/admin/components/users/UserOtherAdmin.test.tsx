import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { apiClient } from "@/lib/api-client";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUserPage } from "./AdminUserPage";
import { AdminUsers } from "./AdminUsers";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const OTHER_ADMIN = {
    ...ADMIN_SESSION_USER,
    id: "0192f3a0-0000-7000-8000-0000000000ae",
    username: "admin.lan",
    displayName: "Admin Lan",
};
const TARGET = "admin.huy";

function renderPage(search = "") {
    visit(`/admin/users/${TARGET}${search}`);
    renderWithProviders(
        <ToastProvider>
            <AdminUserPage username={TARGET} />
        </ToastProvider>,
    );
}

function renderUsers(search: string) {
    visit(`/admin/users${search}`);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
}

describe("thao tác trên tài khoản quản trị viên khác", () => {
    beforeEach(() => {
        writeMockSession(OTHER_ADMIN);
    });

    it("bảng chi tiết nhanh không có nút khoá nhưng vẫn gửi được email đặt lại mật khẩu", async () => {
        renderUsers(`?user=${TARGET}`);
        const panel = within(await screen.findByRole("dialog", { name: "Chi tiết người dùng" }));
        await panel.findByRole("button", { name: "Gửi email đặt lại mật khẩu" });

        expect(panel.queryByRole("button", { name: "Khoá tài khoản" })).not.toBeInTheDocument();
    });

    it("trang chi tiết chỉ cho sửa, không cho khoá hay xoá", async () => {
        renderPage();
        const card = within(await screen.findByRole("region", { name: "Quyền và trạng thái" }));
        await card.findByRole("spinbutton", { name: "Hạn mức AI mỗi ngày" });
        await screen.findByRole("button", { name: "Sửa" });

        expect(screen.queryByRole("button", { name: "Khoá tạm thời" })).not.toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Khoá vĩnh viễn" })).not.toBeInTheDocument();
        expect(screen.queryByRole("button", { name: "Xoá tài khoản" })).not.toBeInTheDocument();
    });

    it("hộp sửa khoá ô vai trò kèm lời giải thích", async () => {
        renderPage();
        fireEvent.click(await screen.findByRole("button", { name: "Sửa" }));
        const dialog = within(await screen.findByRole("dialog", { name: `Sửa ${TARGET}` }));

        expect(await dialog.findByRole("combobox", { name: "Vai trò" })).toBeDisabled();
        expect(
            dialog.getByText("Không thể đổi vai trò của quản trị viên khác."),
        ).toBeInTheDocument();
    });

    it("khoá khối quyền và hạn chế", async () => {
        renderPage();
        const card = within(await screen.findByRole("region", { name: "Quyền và trạng thái" }));
        const quota = await card.findByRole("spinbutton", { name: "Hạn mức AI mỗi ngày" });

        expect(quota).toBeDisabled();
        expect(card.getByRole("combobox", { name: "Vai trò" })).toBeDisabled();
        expect(
            card.getByText("Không thể đổi quyền hoặc hạn chế của quản trị viên khác."),
        ).toBeInTheDocument();
        expect(card.queryByRole("button", { name: "Lưu thay đổi" })).not.toBeInTheDocument();
    });

    it("chỉ cho xem phiên đăng nhập, không cho đăng xuất", async () => {
        renderPage("?tab=sessions");
        const sessions = within(await screen.findByRole("region", { name: "Phiên đăng nhập" }));
        await sessions.findAllByRole("listitem");

        expect(sessions.queryByRole("button", { name: /^Đăng xuất/ })).not.toBeInTheDocument();
    });

    it("dòng trong bảng không có nút xoá", async () => {
        renderUsers("?page=4");
        await screen.findByRole("button", { name: `Sửa ${TARGET}` });

        await waitFor(() =>
            expect(screen.queryByRole("button", { name: `Xoá ${TARGET}` })).not.toBeInTheDocument(),
        );
    });

    it("API giả từ chối khoá quản trị viên khác bằng mã CANNOT_MODIFY_ADMIN", async () => {
        await expect(
            apiClient.post(`/api/admin/users/${TARGET}/ban`, {
                duration: "PERMANENT",
                reason: "Thử",
                notifyByEmail: false,
            }),
        ).rejects.toMatchObject({ status: 422, code: "CANNOT_MODIFY_ADMIN" });
    });
});
