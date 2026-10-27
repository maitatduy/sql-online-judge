import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUsers } from "../users/AdminUsers";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function openInviteDialog() {
    visit("/admin/users");
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    fireEvent.click(await screen.findByRole("button", { name: "Mời người ra đề" }));
    return within(await screen.findByRole("dialog", { name: "Mời người ra đề" }));
}

function search(dialog: ReturnType<typeof within>, text: string) {
    fireEvent.change(dialog.getByRole("searchbox", { name: "Tìm người dùng" }), {
        target: { value: text },
    });
}

const suggestion = (dialog: ReturnType<typeof within>, username: string) =>
    dialog.findByRole("button", { name: new RegExp(`^${username.replace(".", "\\.")}`) });

const invitees = (dialog: ReturnType<typeof within>) =>
    within(dialog.getByRole("list", { name: "Người sẽ được mời" }));

describe("hộp mời người ra đề", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("tìm theo tên người dùng hoặc email; người không mời được bị mờ kèm lý do", async () => {
        const dialog = await openInviteDialog();

        search(dialog, "viet");
        expect(await suggestion(dialog, "viet.luu")).toBeEnabled();

        search(dialog, "admin");
        const admin = await suggestion(dialog, "admin.huy");
        expect(admin).toBeDisabled();
        expect(admin).toHaveTextContent("Là quản trị viên");
    });

    it("chọn người thì thêm xuống danh sách, bấm bỏ thì xoá khỏi danh sách", async () => {
        const dialog = await openInviteDialog();
        expect(dialog.getByRole("button", { name: "Gửi lời mời" })).toBeDisabled();

        search(dialog, "viet");
        fireEvent.click(await suggestion(dialog, "viet.luu"));

        expect(invitees(dialog).getByText("viet.luu")).toBeInTheDocument();
        expect(dialog.getByRole("button", { name: "Gửi 1 lời mời" })).toBeEnabled();

        fireEvent.click(dialog.getByRole("button", { name: "Bỏ viet.luu" }));

        expect(dialog.getByRole("button", { name: "Gửi lời mời" })).toBeDisabled();
    });

    it("chọn xong thì ô tìm được xoá và gợi ý biến mất, tìm lại thì gợi ý hiện lại", async () => {
        const dialog = await openInviteDialog();

        search(dialog, "viet");
        fireEvent.click(await suggestion(dialog, "viet.luu"));

        expect(dialog.getByRole("searchbox", { name: "Tìm người dùng" })).toHaveValue("");
        expect(dialog.queryByRole("list", { name: "Gợi ý" })).not.toBeInTheDocument();

        search(dialog, "viet");
        expect(await dialog.findByRole("list", { name: "Gợi ý" })).toBeInTheDocument();
    });

    it("gõ email chưa có tài khoản thì mời được email đó", async () => {
        const dialog = await openInviteDialog();

        search(dialog, "Ban.Moi@Gmail.com");
        fireEvent.click(
            await dialog.findByRole("button", { name: "Mời email này: ban.moi@gmail.com" }),
        );

        expect(invitees(dialog).getByText("ban.moi@gmail.com")).toBeInTheDocument();
    });

    it("email đang có lời mời chờ thì bị mờ", async () => {
        server.use(
            http.get("*/api/admin/role-invitations", () =>
                HttpResponse.json({
                    items: [
                        {
                            id: "inv-1",
                            email: "hoa.kieu@example.com",
                            role: "SETTER",
                            status: "PENDING",
                            inviterUsername: "admin.huy",
                            invitedAt: "2026-10-24T05:00:00Z",
                            expiresAt: "2026-10-31T05:00:00Z",
                            acceptedAt: null,
                            revokedAt: null,
                        },
                    ],
                    page: 1,
                    size: 100,
                    totalItems: 1,
                    totalPages: 1,
                }),
            ),
        );
        const dialog = await openInviteDialog();

        search(dialog, "hoa");
        await waitFor(async () =>
            expect(await suggestion(dialog, "hoa.kieu")).toHaveTextContent("Đang chờ nhận lời mời"),
        );
        expect(await suggestion(dialog, "hoa.kieu")).toBeDisabled();
    });

    it("gửi lời mời theo vai trò và lời nhắn, rồi hiện số đã gửi và người bị bỏ qua", async () => {
        let sentBody: unknown;
        server.use(
            http.post("*/api/admin/role-invitations", async ({ request }) => {
                sentBody = await request.json();
                return HttpResponse.json({
                    sentCount: 1,
                    skipped: [{ email: "ban.moi@gmail.com", reason: "Đang có lời mời chờ nhận" }],
                });
            }),
        );
        const dialog = await openInviteDialog();
        fireEvent.change(dialog.getByRole("combobox", { name: "Vai trò" }), {
            target: { value: "MODERATOR" },
        });
        search(dialog, "viet");
        fireEvent.click(await suggestion(dialog, "viet.luu"));
        search(dialog, "ban.moi@gmail.com");
        fireEvent.click(
            await dialog.findByRole("button", { name: "Mời email này: ban.moi@gmail.com" }),
        );
        fireEvent.change(dialog.getByRole("textbox", { name: "Lời nhắn" }), {
            target: { value: "Kiểm duyệt cộng đồng" },
        });

        fireEvent.click(dialog.getByRole("button", { name: "Gửi 2 lời mời" }));

        expect(await dialog.findByText("Đã gửi 1 lời mời.")).toBeInTheDocument();
        expect(dialog.getByText("ban.moi@gmail.com")).toBeInTheDocument();
        expect(dialog.getByText("Đang có lời mời chờ nhận")).toBeInTheDocument();
        expect(sentBody).toEqual({
            emails: ["viet.luu@example.com", "ban.moi@gmail.com"],
            role: "MODERATOR",
            message: "Kiểm duyệt cộng đồng",
        });
    });
});
