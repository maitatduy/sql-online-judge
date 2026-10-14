import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUsers } from "./AdminUsers";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const SUBMIT = "Xoá tài khoản";

async function renderUsers(search = "") {
    visit(`/admin/users${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    await screen.findByText(/^\d+-\d+ trong \d+$/);
}

async function openDeleteDialog(username: string) {
    fireEvent.click(screen.getByRole("button", { name: `Xoá ${username}` }));
    return within(await screen.findByRole("dialog", { name: `Xoá ${username}?` }));
}

function typeReason(dialog: ReturnType<typeof within>, reason: string) {
    fireEvent.change(dialog.getByRole("textbox", { name: /^Lý do/ }), {
        target: { value: reason },
    });
}

describe("xoá người dùng", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("có nút xoá ở mỗi dòng trừ tài khoản của chính mình", async () => {
        await renderUsers("?page=4");
        await screen.findByRole("button", { name: "Sửa admin.huy" });

        expect(screen.getByRole("button", { name: "Xoá quang.le" })).toBeInTheDocument();
        await waitFor(() =>
            expect(screen.queryByRole("button", { name: "Xoá admin.huy" })).not.toBeInTheDocument(),
        );
    });

    it("hỏi xác nhận kèm lý do bắt buộc", async () => {
        await renderUsers();
        const dialog = await openDeleteDialog("tam.nguyen");

        expect(screen.getByRole("dialog")).toHaveTextContent(
            "Tài khoản sẽ bị ẩn khỏi danh sách và không đăng nhập được nữa. Bài nộp và lịch sử vẫn được giữ để đối soát.",
        );
        expect(dialog.getByRole("button", { name: SUBMIT })).toBeDisabled();
    });

    it("xoá tài khoản, gửi lý do đã cắt khoảng trắng và cập nhật bảng cùng thẻ số", async () => {
        const captured: { body?: unknown } = {};
        server.use(
            http.delete("*/api/admin/users/tam.nguyen", async ({ request }) => {
                captured.body = await request.clone().json();
                return undefined;
            }),
        );
        await renderUsers();
        const dialog = await openDeleteDialog("tam.nguyen");

        typeReason(dialog, "  Tài khoản tạo nhầm  ");
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(await screen.findByText("Đã xoá tam.nguyen.")).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(captured.body).toEqual({ reason: "Tài khoản tạo nhầm" });
        await waitFor(() =>
            expect(screen.queryByRole("row", { name: "tam.nguyen" })).not.toBeInTheDocument(),
        );
        expect(
            within(screen.getByRole("button", { name: /^Tổng tài khoản/ })).getByText("61"),
        ).toBeInTheDocument();
    });

    it("bỏ tài khoản đã xoá khỏi danh sách đang chọn", async () => {
        await renderUsers();
        fireEvent.click(screen.getByRole("checkbox", { name: "Chọn tam.nguyen" }));
        fireEvent.click(screen.getByRole("checkbox", { name: "Chọn datahust" }));
        const dialog = await openDeleteDialog("tam.nguyen");

        typeReason(dialog, "Trùng");
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        await screen.findByText("Đã xoá tam.nguyen.");
        expect(
            within(screen.getByRole("region", { name: "Thao tác hàng loạt" })).getByText(
                "Đã chọn 1",
            ),
        ).toBeInTheDocument();
    });

    it("báo lỗi bằng toast và giữ hộp thoại khi xoá thất bại", async () => {
        server.use(
            http.delete("*/api/admin/users/tam.nguyen", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        await renderUsers();
        const dialog = await openDeleteDialog("tam.nguyen");

        typeReason(dialog, "Trùng");
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(
            await screen.findByText("Chưa xoá được tam.nguyen. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(screen.getByRole("dialog", { name: "Xoá tam.nguyen?" })).toBeInTheDocument();
    });
});
