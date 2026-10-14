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

const DIALOG_NAME = "Mời người ra đề";
const SUBMIT = "Gửi lời mời";

async function openInviteDialog() {
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    fireEvent.click(screen.getByRole("button", { name: DIALOG_NAME }));
    return within(await screen.findByRole("dialog", { name: DIALOG_NAME }));
}

function typeEmails(dialog: ReturnType<typeof within>, emails: string) {
    fireEvent.change(dialog.getByRole("textbox", { name: "Email" }), {
        target: { value: emails },
    });
}

describe("mời người ra đề", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("mở hộp thoại với vai trò Người ra đề và nút gửi bị vô hiệu khi chưa có email", async () => {
        const dialog = await openInviteDialog();

        expect(dialog.getByRole("combobox", { name: "Vai trò" })).toHaveValue("SETTER");
        expect(
            within(dialog.getByRole("combobox", { name: "Vai trò" }))
                .getAllByRole("option")
                .map((option) => option.textContent),
        ).toEqual(["Người ra đề", "Kiểm duyệt"]);
        expect(dialog.getByRole("button", { name: SUBMIT })).toBeDisabled();
    });

    it("gửi lời mời theo từng dòng email và báo số lời mời", async () => {
        let sentBody: unknown;
        server.use(
            http.post("*/api/admin/user-invitations", async ({ request }) => {
                sentBody = await request.clone().json();
                return undefined;
            }),
        );
        const dialog = await openInviteDialog();

        typeEmails(dialog, "a@example.com\nb@example.com, c@example.com\na@example.com");
        fireEvent.change(dialog.getByRole("combobox", { name: "Vai trò" }), {
            target: { value: "MODERATOR" },
        });
        fireEvent.change(dialog.getByRole("textbox", { name: "Lời nhắn" }), {
            target: { value: "  Chào mừng CLB  " },
        });
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(await screen.findByText("Đã gửi 3 lời mời.")).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(sentBody).toEqual({
            emails: ["a@example.com", "b@example.com", "c@example.com"],
            role: "MODERATOR",
            message: "Chào mừng CLB",
        });
    });

    it("không gửi lời nhắn khi để trống", async () => {
        let sentBody: unknown;
        server.use(
            http.post("*/api/admin/user-invitations", async ({ request }) => {
                sentBody = await request.clone().json();
                return undefined;
            }),
        );
        const dialog = await openInviteDialog();

        typeEmails(dialog, "a@example.com");
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        await waitFor(() =>
            expect(sentBody).toEqual({ emails: ["a@example.com"], role: "SETTER" }),
        );
    });

    it("báo các email sai định dạng ngay tại ô và không gửi", async () => {
        let requestCount = 0;
        server.use(
            http.post("*/api/admin/user-invitations", () => {
                requestCount += 1;
                return undefined;
            }),
        );
        const dialog = await openInviteDialog();

        typeEmails(dialog, "a@example.com\nkhong-hop-le\nb@");
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(await dialog.findByText("Email không hợp lệ: khong-hop-le, b@")).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: "Email" })).toBeInvalid();
        expect(requestCount).toBe(0);
    });

    it("gắn lỗi của máy chủ vào ô email", async () => {
        server.use(
            http.post("*/api/admin/user-invitations", () =>
                HttpResponse.json(
                    {
                        status: 400,
                        code: "VALIDATION_FAILED",
                        message: "Dữ liệu gửi lên không hợp lệ",
                        errors: [
                            { field: "emails", message: "Email đã có tài khoản: a@example.com" },
                        ],
                    },
                    { status: 400 },
                ),
            ),
        );
        const dialog = await openInviteDialog();

        typeEmails(dialog, "a@example.com");
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(await dialog.findByText("Email đã có tài khoản: a@example.com")).toBeInTheDocument();
    });

    it("báo lỗi bằng toast và giữ hộp thoại khi gửi thất bại", async () => {
        server.use(
            http.post("*/api/admin/user-invitations", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        const dialog = await openInviteDialog();

        typeEmails(dialog, "a@example.com");
        fireEvent.click(dialog.getByRole("button", { name: SUBMIT }));

        expect(
            await screen.findByText("Chưa gửi được lời mời. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: "Email" })).toHaveValue("a@example.com");
    });
});
