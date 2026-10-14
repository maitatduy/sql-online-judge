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

const SELF_NOTICE = "Tài khoản của bạn, admin.huy, sẽ được bỏ qua.";

async function renderWithSelection(search: string, usernames: string[]) {
    visit(`/admin/users${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    await screen.findByRole("row", { name: usernames[0] });
    usernames.forEach((username) =>
        fireEvent.click(screen.getByRole("checkbox", { name: `Chọn ${username}` })),
    );
    return within(screen.getByRole("region", { name: "Thao tác hàng loạt" }));
}

async function openDialog(bar: ReturnType<typeof within>, action: string, title: string) {
    fireEvent.click(bar.getByRole("button", { name: action }));
    return within(await screen.findByRole("dialog", { name: title }));
}

function typeReason(dialog: ReturnType<typeof within>, reason: string) {
    fireEvent.change(dialog.getByRole("textbox", { name: /^Lý do/ }), {
        target: { value: reason },
    });
}

const STATUS_CELL_INDEX = 2;

const statusOf = (username: string) =>
    within(screen.getByRole("row", { name: username })).getAllByRole("cell")[STATUS_CELL_INDEX];

describe("khoá hàng loạt", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("khoá các tài khoản đã chọn, bỏ chọn và cập nhật bảng cùng thẻ số", async () => {
        const bar = await renderWithSelection("", ["tam.nguyen", "datahust"]);
        const dialog = await openDialog(bar, "Khoá tài khoản", "Khoá 2 tài khoản");

        expect(dialog.queryByText(SELF_NOTICE)).not.toBeInTheDocument();

        typeReason(dialog, "Tài khoản spam");
        fireEvent.click(dialog.getByRole("button", { name: "Khoá tài khoản" }));

        expect(
            await screen.findByText("Đã khoá 2 tài khoản. Thao tác đã được ghi vào nhật ký."),
        ).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(
            screen.queryByRole("region", { name: "Thao tác hàng loạt" }),
        ).not.toBeInTheDocument();
        await waitFor(() => expect(statusOf("datahust")).toHaveTextContent("Bị khoá"));
        expect(statusOf("tam.nguyen")).toHaveTextContent("Bị khoá");
        await waitFor(() =>
            expect(
                within(screen.getByRole("button", { name: /^Đang bị khoá/ })).getByText("4"),
            ).toBeInTheDocument(),
        );
    });

    it("gửi danh sách đã chọn trừ chính mình và báo sẽ bỏ qua chính mình", async () => {
        let sentBody: unknown;
        server.use(
            http.post("*/api/admin/users/bulk-ban", async ({ request }) => {
                sentBody = await request.clone().json();
                return undefined;
            }),
        );
        const bar = await renderWithSelection("?page=4", ["quang.le", "admin.huy"]);
        const dialog = await openDialog(bar, "Khoá tài khoản", "Khoá 1 tài khoản");

        expect(dialog.getByText(SELF_NOTICE)).toBeInTheDocument();

        fireEvent.click(dialog.getByRole("radio", { name: "Vĩnh viễn" }));
        typeReason(dialog, "  Gian lận  ");
        fireEvent.click(dialog.getByRole("button", { name: "Khoá tài khoản" }));

        await waitFor(() =>
            expect(sentBody).toEqual({
                usernames: ["quang.le"],
                duration: "PERMANENT",
                reason: "Gian lận",
                notifyByEmail: true,
            }),
        );
        expect(
            await screen.findByText("Đã khoá 1 tài khoản. Thao tác đã được ghi vào nhật ký."),
        ).toBeInTheDocument();
    });

    it("không cho gửi khi chỉ chọn chính mình", async () => {
        const bar = await renderWithSelection("?page=4", ["admin.huy"]);
        const dialog = await openDialog(bar, "Khoá tài khoản", "Khoá 0 tài khoản");

        typeReason(dialog, "Thử");

        expect(dialog.getByRole("button", { name: "Khoá tài khoản" })).toBeDisabled();
    });

    it("nêu số tài khoản bị bỏ qua", async () => {
        const bar = await renderWithSelection("", ["tam.nguyen", "spam.bot88"]);
        const dialog = await openDialog(bar, "Khoá tài khoản", "Khoá 2 tài khoản");

        typeReason(dialog, "Spam");
        fireEvent.click(dialog.getByRole("button", { name: "Khoá tài khoản" }));

        expect(
            await screen.findByText(
                "Đã khoá 1 tài khoản, bỏ qua 1 tài khoản. Thao tác đã được ghi vào nhật ký.",
            ),
        ).toBeInTheDocument();
    });

    it("báo lỗi, giữ hộp thoại và lựa chọn khi khoá thất bại", async () => {
        server.use(
            http.post("*/api/admin/users/bulk-ban", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        const bar = await renderWithSelection("", ["tam.nguyen", "datahust"]);
        const dialog = await openDialog(bar, "Khoá tài khoản", "Khoá 2 tài khoản");

        typeReason(dialog, "Spam");
        fireEvent.click(dialog.getByRole("button", { name: "Khoá tài khoản" }));

        expect(
            await screen.findByText("Chưa khoá được 2 tài khoản. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(screen.getByRole("dialog")).toBeInTheDocument();
        expect(bar.getByText("Đã chọn 2")).toBeInTheDocument();
    });
});

describe("đổi vai trò hàng loạt", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("đổi vai trò các tài khoản đã chọn rồi bỏ chọn", async () => {
        const bar = await renderWithSelection("", ["tam.nguyen", "datahust"]);
        const dialog = await openDialog(bar, "Đổi vai trò", "Đổi vai trò 2 tài khoản");
        const roleSelect = dialog.getByRole("combobox", { name: "Vai trò mới" });

        expect(
            within(roleSelect)
                .getAllByRole("option")
                .map((option) => option.textContent),
        ).toEqual(["Học viên", "Người ra đề", "Kiểm duyệt"]);

        expect(roleSelect).toHaveValue("SETTER");
        expect(dialog.getByRole("button", { name: "Đổi vai trò" })).toBeDisabled();

        fireEvent.change(roleSelect, { target: { value: "MODERATOR" } });
        typeReason(dialog, "Bổ sung người kiểm duyệt");
        fireEvent.click(dialog.getByRole("button", { name: "Đổi vai trò" }));

        expect(
            await screen.findByText("Đã đổi vai trò 2 tài khoản thành Kiểm duyệt."),
        ).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(
            screen.queryByRole("region", { name: "Thao tác hàng loạt" }),
        ).not.toBeInTheDocument();
        await waitFor(() =>
            expect(
                within(screen.getByRole("row", { name: "tam.nguyen" })).getByText("Kiểm duyệt"),
            ).toBeInTheDocument(),
        );
    });

    it("báo sẽ bỏ qua chính mình", async () => {
        const bar = await renderWithSelection("?page=4", ["quang.le", "admin.huy"]);
        const dialog = await openDialog(bar, "Đổi vai trò", "Đổi vai trò 1 tài khoản");

        expect(dialog.getByText(SELF_NOTICE)).toBeInTheDocument();
    });

    it("báo lỗi và giữ hộp thoại khi đổi thất bại", async () => {
        server.use(
            http.post("*/api/admin/users/bulk-role", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        const bar = await renderWithSelection("", ["tam.nguyen"]);
        const dialog = await openDialog(bar, "Đổi vai trò", "Đổi vai trò 1 tài khoản");

        typeReason(dialog, "Thử");
        fireEvent.click(dialog.getByRole("button", { name: "Đổi vai trò" }));

        expect(
            await screen.findByText("Chưa đổi được vai trò 1 tài khoản. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(screen.getByRole("dialog")).toBeInTheDocument();
    });
});
