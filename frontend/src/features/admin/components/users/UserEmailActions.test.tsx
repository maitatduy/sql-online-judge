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

const RESEND_BUTTON = "Gửi lại email xác thực";
const RESET_BUTTON = "Gửi email đặt lại mật khẩu";
const RESET_DIALOG = "Gửi email đặt lại mật khẩu?";

async function openPanel(username: string) {
    visit(`/admin/users?user=${username}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    const panel = within(await screen.findByRole("complementary", { name: "Chi tiết người dùng" }));
    await panel.findByRole("button", { name: RESET_BUTTON });
    return panel;
}

function serverError() {
    return HttpResponse.json(
        { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
        { status: 500 },
    );
}

describe("gửi lại email xác thực từ panel", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("chỉ hiện nút với tài khoản chưa xác minh email", async () => {
        const panel = await openPanel("viet.luu");

        expect(panel.queryByRole("button", { name: RESEND_BUTTON })).not.toBeInTheDocument();
    });

    it("gửi lại email và báo địa chỉ nhận", async () => {
        const panel = await openPanel("tam.nguyen");

        fireEvent.click(panel.getByRole("button", { name: RESEND_BUTTON }));

        expect(
            await screen.findByText("Đã gửi lại email xác thực tới tam.nguyen@example.com."),
        ).toBeInTheDocument();
    });

    it("báo lỗi bằng toast khi gửi thất bại", async () => {
        server.use(
            http.post("*/api/admin/users/tam.nguyen/verification-email", () => serverError()),
        );
        const panel = await openPanel("tam.nguyen");

        fireEvent.click(panel.getByRole("button", { name: RESEND_BUTTON }));

        expect(
            await screen.findByText(
                "Chưa gửi được email xác thực tới tam.nguyen@example.com. Đã có lỗi xảy ra",
            ),
        ).toBeInTheDocument();
        expect(panel.getByRole("button", { name: RESEND_BUTTON })).toBeEnabled();
    });
});

describe("gửi email đặt lại mật khẩu từ panel", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("hỏi xác nhận, nêu rõ email nhận và thời hạn của liên kết", async () => {
        const panel = await openPanel("viet.luu");

        fireEvent.click(panel.getByRole("button", { name: RESET_BUTTON }));
        const dialog = await screen.findByRole("dialog", { name: RESET_DIALOG });

        expect(dialog).toHaveTextContent(
            "Liên kết có hiệu lực 30 phút sẽ được gửi tới viet.luu@example.com.",
        );
        expect(dialog).toHaveTextContent(
            "Mật khẩu hiện tại vẫn dùng được cho tới khi người dùng đổi.",
        );
    });

    it("gửi email sau khi xác nhận rồi đóng hộp thoại", async () => {
        const panel = await openPanel("viet.luu");

        fireEvent.click(panel.getByRole("button", { name: RESET_BUTTON }));
        const dialog = within(await screen.findByRole("dialog", { name: RESET_DIALOG }));
        fireEvent.click(dialog.getByRole("button", { name: "Gửi email" }));

        expect(
            await screen.findByText("Đã gửi email đặt lại mật khẩu tới viet.luu@example.com."),
        ).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    });

    it("không gửi gì khi bấm Huỷ", async () => {
        let requestCount = 0;
        server.use(
            http.post("*/api/admin/users/viet.luu/password-reset-email", () => {
                requestCount += 1;
                return undefined;
            }),
        );
        const panel = await openPanel("viet.luu");

        fireEvent.click(panel.getByRole("button", { name: RESET_BUTTON }));
        const dialog = within(await screen.findByRole("dialog", { name: RESET_DIALOG }));
        fireEvent.click(dialog.getByRole("button", { name: "Huỷ" }));

        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(requestCount).toBe(0);
    });

    it("báo lỗi bằng toast và giữ hộp thoại khi gửi thất bại", async () => {
        server.use(
            http.post("*/api/admin/users/viet.luu/password-reset-email", () => serverError()),
        );
        const panel = await openPanel("viet.luu");

        fireEvent.click(panel.getByRole("button", { name: RESET_BUTTON }));
        const dialog = within(await screen.findByRole("dialog", { name: RESET_DIALOG }));
        fireEvent.click(dialog.getByRole("button", { name: "Gửi email" }));

        expect(
            await screen.findByText(
                "Chưa gửi được email đặt lại mật khẩu tới viet.luu@example.com. Đã có lỗi xảy ra",
            ),
        ).toBeInTheDocument();
        expect(dialog.getByRole("button", { name: "Gửi email" })).toBeEnabled();
    });

    it("gửi được cho cả tài khoản đang bị khoá", async () => {
        const panel = await openPanel("copy.paste");

        expect(panel.getByRole("button", { name: RESET_BUTTON })).toBeInTheDocument();
    });
});
