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

const BAN_BUTTON = "Khoá tài khoản";
const UNBAN_BUTTON = "Mở khoá tài khoản";

async function openPanel(username: string) {
    visit(`/admin/users?user=${username}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    const panel = within(await screen.findByRole("dialog", { name: "Chi tiết người dùng" }));
    await panel.findByRole("link", { name: "Mở trang chi tiết" });
    return panel;
}

async function openBanDialog(username: string) {
    const panel = await openPanel(username);
    fireEvent.click(await panel.findByRole("button", { name: BAN_BUTTON }));
    const dialog = within(await screen.findByRole("dialog", { name: `Khoá ${username}` }));
    return { panel, dialog };
}

function typeReason(dialog: ReturnType<typeof within>, reason: string) {
    fireEvent.change(dialog.getByRole("textbox", { name: /^Lý do/ }), {
        target: { value: reason },
    });
}

describe("khoá tài khoản từ panel", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("hiện nút khoá với tài khoản đang hoạt động", async () => {
        const panel = await openPanel("viet.luu");

        expect(await panel.findByRole("button", { name: BAN_BUTTON })).toBeInTheDocument();
        expect(panel.queryByRole("button", { name: UNBAN_BUTTON })).not.toBeInTheDocument();
    });

    it("hiện nút mở khoá thay cho nút khoá với tài khoản đang bị khoá", async () => {
        const panel = await openPanel("copy.paste");

        expect(panel.getByRole("button", { name: UNBAN_BUTTON })).toBeInTheDocument();
        expect(panel.queryByRole("button", { name: BAN_BUTTON })).not.toBeInTheDocument();
    });

    it("không cho khoá tài khoản của chính mình", async () => {
        const panel = await openPanel("admin.huy");
        await panel.findByRole("button", { name: "Gửi email đặt lại mật khẩu" });

        expect(panel.queryByRole("button", { name: BAN_BUTTON })).not.toBeInTheDocument();
    });

    it("mở hộp thoại với thời hạn 7 ngày, gửi email bật và nút khoá bị vô hiệu", async () => {
        const { dialog } = await openBanDialog("viet.luu");

        expect(dialog.getByRole("radio", { name: "7 ngày" })).toBeChecked();
        expect(dialog.getByRole("radio", { name: "30 ngày" })).not.toBeChecked();
        expect(dialog.getByRole("radio", { name: "Vĩnh viễn" })).not.toBeChecked();
        expect(
            dialog.getByRole("checkbox", { name: "Gửi email thông báo cho người dùng" }),
        ).toBeChecked();
        expect(dialog.getByRole("button", { name: BAN_BUTTON })).toBeDisabled();

        typeReason(dialog, "   ");

        expect(dialog.getByRole("button", { name: BAN_BUTTON })).toBeDisabled();
    });

    it("khoá có thời hạn rồi cập nhật panel, lịch sử vi phạm và thẻ số", async () => {
        const { panel, dialog } = await openBanDialog("viet.luu");

        fireEvent.click(dialog.getByRole("radio", { name: "30 ngày" }));
        typeReason(dialog, "Đăng đáp án trong lúc contest đang diễn ra");
        fireEvent.click(dialog.getByRole("button", { name: BAN_BUTTON }));

        expect(
            await screen.findByText("Đã khoá viet.luu. Thao tác đã được ghi vào nhật ký."),
        ).toBeInTheDocument();
        await waitFor(() =>
            expect(screen.queryByRole("dialog", { name: "Khoá viet.luu" })).not.toBeInTheDocument(),
        );
        expect(panel.getByText("Bị khoá")).toBeInTheDocument();
        expect(panel.getByText(/^đến \d{2}\/\d{2}\/\d{4}$/)).toBeInTheDocument();
        expect(panel.getByText(/^Bị khoá đến \d{2}\/\d{2}\/\d{4}$/)).toBeInTheDocument();
        expect(panel.getByText("Đăng đáp án trong lúc contest đang diễn ra")).toBeInTheDocument();
        expect(panel.getByText("Vi phạm").nextElementSibling).toHaveTextContent("3");
        expect(panel.getByRole("button", { name: UNBAN_BUTTON })).toBeInTheDocument();

        fireEvent.click(panel.getByRole("button", { name: "Đóng" }));

        await waitFor(() =>
            expect(
                within(screen.getByRole("button", { name: /^Đang bị khoá/ })).getByText("3"),
            ).toBeInTheDocument(),
        );
    });

    it("khoá vĩnh viễn", async () => {
        const { panel, dialog } = await openBanDialog("viet.luu");

        fireEvent.click(dialog.getByRole("radio", { name: "Vĩnh viễn" }));
        typeReason(dialog, "Tái phạm nhiều lần");
        fireEvent.click(dialog.getByRole("button", { name: BAN_BUTTON }));

        expect(await panel.findByText("vĩnh viễn")).toBeInTheDocument();
        expect(panel.getByText("Bị khoá vĩnh viễn")).toBeInTheDocument();
    });

    it("gửi đúng thời hạn, lý do đã cắt khoảng trắng và lựa chọn gửi email", async () => {
        let sentBody: unknown;
        server.use(
            http.post("*/api/admin/users/viet.luu/ban", async ({ request }) => {
                sentBody = await request.clone().json();
                return undefined;
            }),
        );
        const { dialog } = await openBanDialog("viet.luu");

        typeReason(dialog, "  Spam bình luận  ");
        fireEvent.click(
            dialog.getByRole("checkbox", { name: "Gửi email thông báo cho người dùng" }),
        );
        fireEvent.click(dialog.getByRole("button", { name: BAN_BUTTON }));

        await waitFor(() =>
            expect(sentBody).toEqual({
                duration: "DAYS_7",
                reason: "Spam bình luận",
                notifyByEmail: false,
            }),
        );
        expect(
            await screen.findByText("Đã khoá viet.luu. Thao tác đã được ghi vào nhật ký."),
        ).toBeInTheDocument();
    });

    it("gắn lỗi của máy chủ vào ô lý do và giữ hộp thoại", async () => {
        server.use(
            http.post("*/api/admin/users/viet.luu/ban", () =>
                HttpResponse.json(
                    {
                        status: 400,
                        code: "VALIDATION_FAILED",
                        message: "Dữ liệu gửi lên không hợp lệ",
                        errors: [{ field: "reason", message: "Lý do chứa từ ngữ không phù hợp" }],
                    },
                    { status: 400 },
                ),
            ),
        );
        const { dialog } = await openBanDialog("viet.luu");

        typeReason(dialog, "Lý do");
        fireEvent.click(dialog.getByRole("button", { name: BAN_BUTTON }));

        expect(await dialog.findByText("Lý do chứa từ ngữ không phù hợp")).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: /^Lý do/ })).toBeInvalid();
    });

    it("báo lỗi bằng toast, giữ hộp thoại và lý do khi máy chủ lỗi", async () => {
        server.use(
            http.post("*/api/admin/users/viet.luu/ban", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        const { dialog } = await openBanDialog("viet.luu");

        typeReason(dialog, "Spam bình luận");
        fireEvent.click(dialog.getByRole("button", { name: BAN_BUTTON }));

        expect(
            await screen.findByText("Chưa khoá được viet.luu. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: /^Lý do/ })).toHaveValue("Spam bình luận");
        expect(dialog.getByRole("button", { name: BAN_BUTTON })).toBeEnabled();
    });

    it("đóng hộp thoại bằng nút Huỷ mà không khoá", async () => {
        const { panel, dialog } = await openBanDialog("viet.luu");

        typeReason(dialog, "Nhầm người");
        fireEvent.click(dialog.getByRole("button", { name: "Huỷ" }));

        await waitFor(() =>
            expect(screen.queryByRole("dialog", { name: "Khoá viet.luu" })).not.toBeInTheDocument(),
        );
        expect(panel.getByText("Hoạt động")).toBeInTheDocument();
    });
});

describe("mở khoá tài khoản từ panel", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("mở khoá sau khi nhập lý do rồi cập nhật panel", async () => {
        const panel = await openPanel("copy.paste");

        fireEvent.click(panel.getByRole("button", { name: UNBAN_BUTTON }));
        const dialog = within(await screen.findByRole("dialog", { name: "Mở khoá copy.paste?" }));

        expect(dialog.getByRole("button", { name: "Mở khoá" })).toBeDisabled();

        typeReason(dialog, "Đã hết thời hạn xem xét");
        fireEvent.click(dialog.getByRole("button", { name: "Mở khoá" }));

        expect(await screen.findByText("Đã mở khoá copy.paste.")).toBeInTheDocument();
        await waitFor(() =>
            expect(
                screen.queryByRole("dialog", { name: "Mở khoá copy.paste?" }),
            ).not.toBeInTheDocument(),
        );
        expect(panel.getByText("Hoạt động")).toBeInTheDocument();
        expect(panel.getByRole("button", { name: BAN_BUTTON })).toBeInTheDocument();
        expect(panel.getByText("Nộp bài trùng code người khác")).toBeInTheDocument();
    });

    it("báo lỗi bằng toast và giữ hộp thoại khi mở khoá thất bại", async () => {
        server.use(
            http.post("*/api/admin/users/copy.paste/unban", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        const panel = await openPanel("copy.paste");

        fireEvent.click(panel.getByRole("button", { name: UNBAN_BUTTON }));
        const dialog = within(await screen.findByRole("dialog", { name: "Mở khoá copy.paste?" }));
        typeReason(dialog, "Thử");
        fireEvent.click(dialog.getByRole("button", { name: "Mở khoá" }));

        expect(
            await screen.findByText("Chưa mở khoá được copy.paste. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(screen.getByRole("dialog", { name: "Mở khoá copy.paste?" })).toBeInTheDocument();
    });
});
