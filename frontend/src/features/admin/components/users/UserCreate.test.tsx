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

const DIALOG_NAME = "Tạo người dùng";
const SUBMIT = "Tạo tài khoản";

async function openCreateDialog() {
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    await screen.findByText("1-20 trong 62");
    fireEvent.click(screen.getByRole("button", { name: DIALOG_NAME }));
    return within(await screen.findByRole("dialog", { name: DIALOG_NAME }));
}

function fill(dialog: ReturnType<typeof within>, name: string, value: string) {
    fireEvent.change(dialog.getByRole("textbox", { name }), { target: { value } });
}

function fillValidUser(dialog: ReturnType<typeof within>) {
    fill(dialog, "Tên người dùng", "lan.pham");
    fill(dialog, "Email", "lan.pham@example.com");
}

function conflict(code: string, message: string) {
    return HttpResponse.json({ status: 409, code, message }, { status: 409 });
}

describe("tạo người dùng", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("mặc định vai trò Học viên và chỉ cho tạo khi đã có username và email", async () => {
        const dialog = await openCreateDialog();

        expect(screen.getByRole("dialog")).toHaveTextContent(
            "Người dùng nhận email kích hoạt để tự đặt mật khẩu. Liên kết có hiệu lực 7 ngày.",
        );
        expect(dialog.getByRole("combobox", { name: "Vai trò" })).toHaveValue("LEARNER");
        expect(dialog.getByRole("button", { name: SUBMIT })).toBeDisabled();

        fill(dialog, "Tên người dùng", "lan.pham");

        expect(dialog.getByRole("button", { name: SUBMIT })).toBeDisabled();

        fill(dialog, "Email", "lan.pham@example.com");

        expect(dialog.getByRole("button", { name: SUBMIT })).toBeEnabled();
    });

    it("tạo tài khoản, báo đã gửi email kích hoạt và thêm vào bảng", async () => {
        const captured: { body?: unknown } = {};
        server.use(
            http.post("*/api/admin/users", async ({ request }) => {
                captured.body = await request.clone().json();
                return undefined;
            }),
        );
        const dialog = await openCreateDialog();

        fill(dialog, "Tên người dùng", "  lan.pham  ");
        fill(dialog, "Email", "lan.pham@example.com");
        fill(dialog, "Tên hiển thị", "Phạm Ngọc Lan");
        fireEvent.change(dialog.getByRole("combobox", { name: "Vai trò" }), {
            target: { value: "SETTER" },
        });
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(
            await screen.findByText(
                "Đã tạo tài khoản lan.pham và gửi email kích hoạt tới lan.pham@example.com.",
            ),
        ).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(captured.body).toEqual({
            username: "lan.pham",
            email: "lan.pham@example.com",
            displayName: "Phạm Ngọc Lan",
            role: "SETTER",
        });
        const row = within(await screen.findByRole("row", { name: "lan.pham" }));
        expect(row.getByText("Người ra đề")).toBeInTheDocument();
        expect(row.getByText("Chưa xác minh")).toBeInTheDocument();
    });

    it("không gửi tên hiển thị khi để trống", async () => {
        const captured: { body?: unknown } = {};
        server.use(
            http.post("*/api/admin/users", async ({ request }) => {
                captured.body = await request.clone().json();
                return undefined;
            }),
        );
        const dialog = await openCreateDialog();

        fillValidUser(dialog);
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        await waitFor(() =>
            expect(captured.body).toEqual({
                username: "lan.pham",
                email: "lan.pham@example.com",
                role: "LEARNER",
            }),
        );
        expect(await screen.findByText(/^Đã tạo tài khoản lan\.pham/)).toBeInTheDocument();
    });

    it("báo lỗi ngay tại ô khi username hoặc email sai định dạng", async () => {
        const dialog = await openCreateDialog();

        fill(dialog, "Tên người dùng", "Lan Pham");
        fill(dialog, "Email", "khong-hop-le");
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(
            await dialog.findByText(
                "Tên người dùng gồm 3-30 ký tự: chữ thường, số, dấu chấm hoặc gạch dưới",
            ),
        ).toBeInTheDocument();
        expect(dialog.getByText("Email không hợp lệ")).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: "Tên người dùng" })).toBeInvalid();
    });

    it.each([
        ["USERNAME_TAKEN", "Tên người dùng", "Tên người dùng đã có người dùng"],
        ["EMAIL_TAKEN", "Email", "Email đã được dùng cho tài khoản khác"],
    ])("gắn lỗi %s vào ô %s", async (code, field, message) => {
        server.use(http.post("*/api/admin/users", () => conflict(code, "Đã tồn tại")));
        const dialog = await openCreateDialog();

        fillValidUser(dialog);
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(await dialog.findByText(message)).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: field })).toBeInvalid();
    });

    it("báo lỗi bằng toast và giữ hộp thoại khi máy chủ lỗi", async () => {
        server.use(
            http.post("*/api/admin/users", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        const dialog = await openCreateDialog();

        fillValidUser(dialog);
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(
            await screen.findByText("Chưa tạo được tài khoản lan.pham. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: "Tên người dùng" })).toHaveValue("lan.pham");
    });
});

describe("tạo người dùng vai trò Quản trị", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("hỏi xác nhận riêng trước khi tạo tài khoản Quản trị", async () => {
        const dialog = await openCreateDialog();

        fillValidUser(dialog);
        fireEvent.change(dialog.getByRole("combobox", { name: "Vai trò" }), {
            target: { value: "ADMIN" },
        });
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        const confirm = within(
            await screen.findByRole("dialog", { name: "Cấp quyền Quản trị cho lan.pham?" }),
        );
        fireEvent.click(confirm.getByRole("button", { name: "Cấp quyền Quản trị" }));

        expect(await screen.findByText(/^Đã tạo tài khoản lan\.pham/)).toBeInTheDocument();
    });

    it("báo lỗi khi tên người dùng trùng tên dành riêng của hệ thống", async () => {
        const dialog = await openCreateDialog();

        fill(dialog, "Tên người dùng", "availability");
        fill(dialog, "Email", "a@example.com");
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(
            await dialog.findByText("Tên người dùng này được hệ thống dành riêng"),
        ).toBeInTheDocument();
    });
});
