import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { apiClient } from "@/lib/api-client";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUsers } from "./AdminUsers";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function deleteUser(username: string) {
    writeMockSession(ADMIN_SESSION_USER);
    await apiClient.delete(`/api/admin/users/${username}`, { reason: "Tạo nhầm" });
}

async function renderUsers(search = "") {
    visit(`/admin/users${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    await screen.findByText(/^(\d+-\d+ trong \d+|Không tìm thấy người dùng)$/);
}

const stateSelect = () => screen.getByRole("combobox", { name: "Lọc theo trạng thái" });

describe("tài khoản đã xoá mềm", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("mặc định ẩn tài khoản đã xoá", async () => {
        await deleteUser("tam.nguyen");
        await renderUsers();

        expect(screen.queryByRole("row", { name: "tam.nguyen" })).not.toBeInTheDocument();
    });

    it("hiện tài khoản đã xoá kèm ngày xoá khi lọc Đã xoá", async () => {
        await deleteUser("tam.nguyen");
        await renderUsers();

        fireEvent.change(stateSelect(), { target: { value: "DELETED" } });

        const row = within(await screen.findByRole("row", { name: "tam.nguyen" }));

        expect(window.location.search).toBe("?status=DELETED");
        expect(row.getByText("Đã xoá")).toBeInTheDocument();
        expect(row.getByText(/^Ngày \d{2}\/\d{2}\/\d{4}$/)).toBeInTheDocument();
        expect(row.getByRole("button", { name: "Khôi phục tam.nguyen" })).toBeInTheDocument();
        expect(row.queryByRole("button", { name: "Sửa tam.nguyen" })).not.toBeInTheDocument();
        expect(row.queryByRole("button", { name: "Xoá tam.nguyen" })).not.toBeInTheDocument();
    });

    it("khôi phục tài khoản rồi đưa lại vào danh sách", async () => {
        await deleteUser("tam.nguyen");
        await renderUsers("?status=DELETED");

        fireEvent.click(await screen.findByRole("button", { name: "Khôi phục tam.nguyen" }));
        const dialog = within(await screen.findByRole("dialog", { name: "Khôi phục tam.nguyen?" }));
        fireEvent.click(dialog.getByRole("button", { name: "Khôi phục" }));

        expect(await screen.findByText("Đã khôi phục tam.nguyen.")).toBeInTheDocument();
        expect(await screen.findByText("Không tìm thấy người dùng")).toBeInTheDocument();

        fireEvent.change(stateSelect(), { target: { value: "" } });

        expect(await screen.findByRole("row", { name: "tam.nguyen" })).toBeInTheDocument();
    });

    it("xem được chi tiết tài khoản đã xoá nhưng không có thao tác khoá hay gửi email", async () => {
        await deleteUser("viet.luu");
        await renderUsers("?status=DELETED&user=viet.luu");

        const detail = within(await screen.findByRole("dialog", { name: "Chi tiết người dùng" }));

        expect(await detail.findByText("Lưu Quốc Việt")).toBeInTheDocument();
        expect(detail.getByText("Đã xoá")).toBeInTheDocument();
        expect(detail.queryByRole("button", { name: "Khoá tài khoản" })).not.toBeInTheDocument();
        expect(
            detail.queryByRole("button", { name: "Gửi email đặt lại mật khẩu" }),
        ).not.toBeInTheDocument();
    });

    it("không đánh dấu thẻ số nào khi đang xem tài khoản đã xoá", async () => {
        await renderUsers("?status=DELETED");

        await waitFor(() =>
            expect(screen.getByRole("button", { name: /^Tổng tài khoản/ })).toHaveAttribute(
                "aria-pressed",
                "false",
            ),
        );
    });
});
