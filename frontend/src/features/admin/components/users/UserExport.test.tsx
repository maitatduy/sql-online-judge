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

const DIALOG_NAME = "Xuất danh sách người dùng";
const INCLUDE_EMAIL = "Kèm email";
const SUCCESS_TOAST = "Đang tạo file CSV, bạn sẽ nhận thông báo khi xong.";

async function renderUsers(search = "", selectedUsernames: string[] = []) {
    visit(`/admin/users${search}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    await screen.findByText(/^1-\d+ trong \d+$/);
    await screen.findByText("62", { selector: "button span" });
    selectedUsernames.forEach((username) =>
        fireEvent.click(screen.getByRole("checkbox", { name: `Chọn ${username}` })),
    );
}

async function openExportDialog() {
    fireEvent.click(screen.getByRole("button", { name: "Xuất CSV" }));
    return within(await screen.findByRole("dialog", { name: DIALOG_NAME }));
}

function captureExportBody() {
    const captured: { body?: unknown } = {};
    server.use(
        http.post("*/api/admin/user-exports", async ({ request }) => {
            captured.body = await request.clone().json();
            return undefined;
        }),
    );
    return captured;
}

describe("xuất danh sách người dùng", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("mặc định xuất theo bộ lọc hiện tại, không kèm email", async () => {
        await renderUsers();
        const dialog = await openExportDialog();

        expect(screen.getByRole("dialog")).toHaveTextContent(
            "File CSV sẽ có trong mục Thông báo khi tạo xong.",
        );
        expect(dialog.getByRole("radio", { name: "Theo bộ lọc hiện tại: 62 người" })).toBeChecked();
        expect(dialog.getByRole("radio", { name: "Những người đã chọn: 0 người" })).toBeDisabled();
        expect(dialog.getByRole("radio", { name: "Tất cả 62 tài khoản" })).not.toBeChecked();
        expect(dialog.getByRole("checkbox", { name: INCLUDE_EMAIL })).not.toBeChecked();
    });

    it("gửi đúng bộ lọc đang áp và báo đang tạo file", async () => {
        const captured = captureExportBody();
        await renderUsers("?role=SETTER&page=1");
        const dialog = await openExportDialog();

        expect(dialog.getByRole("radio", { name: "Theo bộ lọc hiện tại: 3 người" })).toBeChecked();

        fireEvent.click(dialog.getByRole("button", { name: "Tạo file CSV" }));

        expect(await screen.findByText(SUCCESS_TOAST)).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        expect(captured.body).toEqual({
            scope: "FILTERED",
            filters: { role: "SETTER" },
            includeEmail: false,
        });
    });

    it("xuất những người đã chọn kèm email", async () => {
        const captured = captureExportBody();
        await renderUsers("", ["tam.nguyen", "datahust"]);
        const dialog = await openExportDialog();

        fireEvent.click(dialog.getByRole("radio", { name: "Những người đã chọn: 2 người" }));
        fireEvent.click(dialog.getByRole("checkbox", { name: INCLUDE_EMAIL }));
        fireEvent.click(dialog.getByRole("button", { name: "Tạo file CSV" }));

        await waitFor(() =>
            expect(captured.body).toEqual({
                scope: "SELECTED",
                usernames: ["tam.nguyen", "datahust"],
                includeEmail: true,
            }),
        );
    });

    it("xuất tất cả tài khoản", async () => {
        const captured = captureExportBody();
        await renderUsers("?q=viet");
        const dialog = await openExportDialog();

        fireEvent.click(dialog.getByRole("radio", { name: "Tất cả 62 tài khoản" }));
        fireEvent.click(dialog.getByRole("button", { name: "Tạo file CSV" }));

        await waitFor(() => expect(captured.body).toEqual({ scope: "ALL", includeEmail: false }));
    });

    it("báo lỗi bằng toast và giữ hộp thoại khi tạo file thất bại", async () => {
        server.use(
            http.post("*/api/admin/user-exports", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        await renderUsers();
        const dialog = await openExportDialog();

        fireEvent.click(dialog.getByRole("button", { name: "Tạo file CSV" }));

        expect(
            await screen.findByText("Chưa tạo được file CSV. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
        expect(screen.getByRole("dialog", { name: DIALOG_NAME })).toBeInTheDocument();
    });
});
