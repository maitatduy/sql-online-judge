import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { apiError } from "@/mocks/api-error";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUsers } from "../users/AdminUsers";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function openInvitations() {
    visit("/admin/users");
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    fireEvent.click(await screen.findByRole("button", { name: "Lời mời 2" }));
    const dialog = within(await screen.findByRole("dialog", { name: "Lời mời" }));
    await dialog.findAllByRole("row");
    return dialog;
}

const rowOf = (dialog: ReturnType<typeof within>, email: string) =>
    dialog.findByRole("row", { name: new RegExp(email.replace(".", "\\.")) });

function showStatus(dialog: ReturnType<typeof within>, value: string) {
    fireEvent.change(dialog.getByRole("combobox", { name: "Trạng thái" }), {
        target: { value },
    });
}

describe("quản lý lời mời", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("nút hiện số lời mời đang chờ; modal mặc định liệt kê lời mời đang chờ", async () => {
        const dialog = await openInvitations();

        const giang = await rowOf(dialog, "giang.vo@gmail.com");
        expect(giang).toHaveTextContent("Người ra đề");
        expect(giang).toHaveTextContent("Đang chờ");
        expect(giang).toHaveTextContent("admin.huy");
        expect(await rowOf(dialog, "linh.tran@gmail.com")).toHaveTextContent("Kiểm duyệt");
        expect(dialog.queryByText("chi.pham@gmail.com")).not.toBeInTheDocument();
    });

    it("đổi trạng thái lọc thì hiện đúng lời mời; đã nhận thì không có nút thao tác", async () => {
        const dialog = await openInvitations();

        showStatus(dialog, "ACCEPTED");

        const accepted = await rowOf(dialog, "an.nguyen@gmail.com");
        expect(accepted).toHaveTextContent("Đã nhận");
        expect(within(accepted).queryByRole("button")).not.toBeInTheDocument();
    });

    it("huỷ lời mời đang chờ: báo đã huỷ, bỏ khỏi danh sách và giảm số trên nút", async () => {
        const dialog = await openInvitations();

        fireEvent.click(
            within(await rowOf(dialog, "giang.vo@gmail.com")).getByRole("button", {
                name: "Huỷ lời mời giang.vo@gmail.com",
            }),
        );

        expect(
            await screen.findByText("Đã huỷ lời mời gửi tới giang.vo@gmail.com."),
        ).toBeInTheDocument();
        await waitFor(() =>
            expect(dialog.queryByText("giang.vo@gmail.com")).not.toBeInTheDocument(),
        );
        expect(
            await screen.findByRole("button", { name: "Lời mời 1", hidden: true }),
        ).toBeInTheDocument();
    });

    it("gửi lại lời mời hết hạn: báo đã gửi và lời mời chuyển sang đang chờ", async () => {
        const dialog = await openInvitations();
        showStatus(dialog, "EXPIRED");

        fireEvent.click(
            within(await rowOf(dialog, "binh.le@gmail.com")).getByRole("button", {
                name: "Gửi lại lời mời binh.le@gmail.com",
            }),
        );

        expect(
            await screen.findByText("Đã gửi lại lời mời tới binh.le@gmail.com."),
        ).toBeInTheDocument();
        await waitFor(() =>
            expect(dialog.queryByText("binh.le@gmail.com")).not.toBeInTheDocument(),
        );
        showStatus(dialog, "PENDING");
        expect(await rowOf(dialog, "binh.le@gmail.com")).toHaveTextContent("Đang chờ");
    });

    it("huỷ lỗi thì báo lỗi kèm lý do từ máy chủ", async () => {
        server.use(
            http.post("*/api/admin/role-invitations/:id/revoke", () =>
                apiError(409, "INVITATION_NOT_PENDING", "Chỉ huỷ được lời mời đang chờ"),
            ),
        );
        const dialog = await openInvitations();

        fireEvent.click(
            within(await rowOf(dialog, "linh.tran@gmail.com")).getByRole("button", {
                name: "Huỷ lời mời linh.tran@gmail.com",
            }),
        );

        expect(
            await screen.findByText("Chưa huỷ được lời mời. Chỉ huỷ được lời mời đang chờ"),
        ).toBeInTheDocument();
    });
});
