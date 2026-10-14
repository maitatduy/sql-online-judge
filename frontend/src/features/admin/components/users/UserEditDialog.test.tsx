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

const SAVE = "Lưu thay đổi";

async function openEditDialog(username: string, search = "") {
    visit(`/admin/users${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    fireEvent.click(await screen.findByRole("button", { name: `Sửa ${username}` }));
    const dialog = within(await screen.findByRole("dialog", { name: `Sửa ${username}` }));
    await dialog.findByRole("textbox", { name: "Tên hiển thị" });
    return dialog;
}

function captureBody() {
    const captured: { body?: unknown } = {};
    server.use(
        http.patch("*/api/admin/users/:username", async ({ request }) => {
            captured.body = await request.clone().json();
            return undefined;
        }),
    );
    return captured;
}

const nameField = (dialog: ReturnType<typeof within>) =>
    dialog.getByRole("textbox", { name: "Tên hiển thị" });
const roleField = (dialog: ReturnType<typeof within>) =>
    dialog.getByRole("combobox", { name: "Vai trò" });

describe("sửa người dùng", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("điền sẵn tên và vai trò hiện tại, nút lưu bị vô hiệu khi chưa đổi gì", async () => {
        const dialog = await openEditDialog("tam.nguyen");

        expect(nameField(dialog)).toHaveValue("Nguyễn Thành Tâm");
        expect(roleField(dialog)).toHaveValue("LEARNER");
        expect(dialog.getByRole("button", { name: SAVE })).toBeDisabled();
    });

    it("lưu tên và vai trò mới rồi cập nhật bảng", async () => {
        const captured = captureBody();
        const dialog = await openEditDialog("tam.nguyen");

        fireEvent.change(nameField(dialog), { target: { value: "  Tâm Nguyễn  " } });
        fireEvent.change(roleField(dialog), { target: { value: "SETTER" } });
        fireEvent.click(dialog.getByRole("button", { name: SAVE }));

        expect(await screen.findByText("Đã cập nhật tam.nguyen.")).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(captured.body).toEqual({ displayName: "Tâm Nguyễn", role: "SETTER" });
        await waitFor(() =>
            expect(
                within(screen.getByRole("row", { name: "tam.nguyen" })).getByText("Người ra đề"),
            ).toBeInTheDocument(),
        );
    });

    it("chỉ gửi trường đã đổi", async () => {
        const captured = captureBody();
        const dialog = await openEditDialog("tam.nguyen");

        fireEvent.change(roleField(dialog), { target: { value: "MODERATOR" } });
        fireEvent.click(dialog.getByRole("button", { name: SAVE }));

        await waitFor(() => expect(captured.body).toEqual({ role: "MODERATOR" }));
        expect(await screen.findByText("Đã cập nhật tam.nguyen.")).toBeInTheDocument();
    });

    it("gửi null khi xoá trống tên hiển thị", async () => {
        const captured = captureBody();
        const dialog = await openEditDialog("tam.nguyen");

        fireEvent.change(nameField(dialog), { target: { value: "   " } });
        fireEvent.click(dialog.getByRole("button", { name: SAVE }));

        await waitFor(() => expect(captured.body).toEqual({ displayName: null }));
        expect(await screen.findByText("Đã cập nhật tam.nguyen.")).toBeInTheDocument();
    });

    it("báo lỗi ngay tại ô khi tên quá dài", async () => {
        const dialog = await openEditDialog("tam.nguyen");

        fireEvent.change(nameField(dialog), { target: { value: "a".repeat(61) } });
        fireEvent.click(dialog.getByRole("button", { name: SAVE }));

        expect(await dialog.findByText("Tên hiển thị tối đa 60 ký tự")).toBeInTheDocument();
        expect(nameField(dialog)).toBeInvalid();
    });

    it("không cho tự đổi vai trò nhưng vẫn cho đổi tên của chính mình", async () => {
        const dialog = await openEditDialog("admin.huy", "?page=4");

        expect(
            dialog.getByText("Bạn không thể tự đổi vai trò của chính mình."),
        ).toBeInTheDocument();
        expect(roleField(dialog)).toBeDisabled();
        expect(nameField(dialog)).toBeEnabled();
    });

    it("báo lỗi bằng toast và giữ hộp thoại khi lưu thất bại", async () => {
        server.use(
            http.patch("*/api/admin/users/tam.nguyen", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        const dialog = await openEditDialog("tam.nguyen");

        fireEvent.change(roleField(dialog), { target: { value: "SETTER" } });
        fireEvent.click(dialog.getByRole("button", { name: SAVE }));

        expect(
            await screen.findByText("Chưa cập nhật được tam.nguyen. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(roleField(dialog)).toHaveValue("SETTER");
    });
});
