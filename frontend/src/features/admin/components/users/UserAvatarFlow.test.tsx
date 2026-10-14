import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { apiClient } from "@/lib/api-client";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUsers } from "./AdminUsers";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const PNG_DATA_URL = "data:image/png;base64,cG5n";
const PREVIEW_ALT = "Xem trước ảnh đại diện";

const pngFile = () => new File(["png"], "avatar.png", { type: "image/png" });

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

async function openCreateDialog() {
    await renderUsers();
    fireEvent.click(screen.getByRole("button", { name: "Tạo người dùng" }));
    return within(await screen.findByRole("dialog", { name: "Tạo người dùng" }));
}

async function openEditDialog(username: string) {
    await renderUsers();
    fireEvent.click(screen.getByRole("button", { name: `Sửa ${username}` }));
    const dialog = within(await screen.findByRole("dialog", { name: `Sửa ${username}` }));
    await dialog.findByRole("textbox", { name: "Tên hiển thị" });
    return dialog;
}

function chooseAvatar(dialog: ReturnType<typeof within>, file: File) {
    fireEvent.change(dialog.getByLabelText("Chọn ảnh"), { target: { files: [file] } });
}

async function uploadAvatarFor(username: string) {
    writeMockSession(ADMIN_SESSION_USER);
    const form = new FormData();
    form.append("file", pngFile());
    await apiClient.putForm(`/api/admin/users/${username}/avatar`, form);
}

describe("form tạo và sửa dùng chung bố cục", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it.each([
        ["tạo", openCreateDialog],
        ["sửa", () => openEditDialog("tam.nguyen")],
    ])("modal %s có đủ ảnh đại diện và các trường", async (_mode, open) => {
        const dialog = await open();

        expect(dialog.getByRole("group", { name: "Ảnh đại diện" })).toBeInTheDocument();
        expect(dialog.getByLabelText("Chọn ảnh")).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: "Tên người dùng" })).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: "Email" })).toBeInTheDocument();
        expect(dialog.getByRole("textbox", { name: "Tên hiển thị" })).toBeInTheDocument();
        expect(dialog.getByRole("combobox", { name: "Vai trò" })).toBeInTheDocument();
    });

    it("chỉ cho xem tên người dùng và email khi sửa", async () => {
        const dialog = await openEditDialog("tam.nguyen");

        expect(dialog.getByRole("textbox", { name: "Tên người dùng" })).toHaveValue("tam.nguyen");
        expect(dialog.getByRole("textbox", { name: "Tên người dùng" })).toHaveAttribute("readonly");
        expect(dialog.getByRole("textbox", { name: "Email" })).toHaveValue(
            "tam.nguyen@example.com",
        );
        expect(dialog.getByRole("textbox", { name: "Email" })).toHaveAttribute("readonly");
    });
});

describe("ảnh đại diện khi tạo người dùng", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("xem trước ảnh đã chọn rồi tải lên cùng tài khoản mới", async () => {
        const dialog = await openCreateDialog();

        chooseAvatar(dialog, pngFile());

        expect(await dialog.findByRole("img", { name: PREVIEW_ALT })).toHaveAttribute(
            "src",
            PNG_DATA_URL,
        );

        fireEvent.change(dialog.getByRole("textbox", { name: "Tên người dùng" }), {
            target: { value: "lan.pham" },
        });
        fireEvent.change(dialog.getByRole("textbox", { name: "Email" }), {
            target: { value: "lan.pham@example.com" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Tạo tài khoản" }));

        expect(await screen.findByText(/^Đã tạo tài khoản lan\.pham/)).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());

        fireEvent.click(await screen.findByRole("button", { name: "Xem chi tiết lan.pham" }));

        const detail = within(await screen.findByRole("dialog", { name: "Chi tiết người dùng" }));

        expect(
            await detail.findByRole("img", { name: "Ảnh đại diện của lan.pham" }),
        ).toHaveAttribute("src", PNG_DATA_URL);
    });

    it("báo lỗi và không xem trước khi chọn tệp không phải ảnh hợp lệ", async () => {
        const dialog = await openCreateDialog();

        chooseAvatar(dialog, new File(["gif"], "a.gif", { type: "image/gif" }));

        expect(await dialog.findByText("Ảnh phải là PNG, JPG hoặc WEBP")).toBeInTheDocument();
        expect(dialog.queryByRole("img", { name: PREVIEW_ALT })).not.toBeInTheDocument();
    });

    it("bỏ ảnh vừa chọn bằng nút Xoá ảnh", async () => {
        const dialog = await openCreateDialog();
        chooseAvatar(dialog, pngFile());
        await dialog.findByRole("img", { name: PREVIEW_ALT });

        fireEvent.click(dialog.getByRole("button", { name: "Xoá ảnh" }));

        expect(dialog.queryByRole("img", { name: PREVIEW_ALT })).not.toBeInTheDocument();
        expect(dialog.queryByRole("button", { name: "Xoá ảnh" })).not.toBeInTheDocument();
    });
});

describe("ảnh đại diện khi sửa người dùng", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("cho lưu khi chỉ đổi ảnh và hiện ảnh mới trong bảng", async () => {
        const dialog = await openEditDialog("tam.nguyen");

        expect(dialog.getByRole("button", { name: "Lưu thay đổi" })).toBeDisabled();

        chooseAvatar(dialog, pngFile());
        await dialog.findByRole("img", { name: PREVIEW_ALT });
        fireEvent.click(dialog.getByRole("button", { name: "Lưu thay đổi" }));

        expect(await screen.findByText("Đã cập nhật tam.nguyen.")).toBeInTheDocument();
        await waitFor(() =>
            expect(
                screen.getByRole("row", { name: "tam.nguyen" }).querySelector("img"),
            ).toHaveAttribute("src", PNG_DATA_URL),
        );
    });

    it("hiện ảnh hiện có và xoá được ảnh", async () => {
        await uploadAvatarFor("tam.nguyen");
        const dialog = await openEditDialog("tam.nguyen");

        expect(dialog.getByRole("img", { name: PREVIEW_ALT })).toHaveAttribute("src", PNG_DATA_URL);

        fireEvent.click(dialog.getByRole("button", { name: "Xoá ảnh" }));
        fireEvent.click(dialog.getByRole("button", { name: "Lưu thay đổi" }));

        expect(await screen.findByText("Đã cập nhật tam.nguyen.")).toBeInTheDocument();
        await waitFor(() =>
            expect(screen.getByRole("row", { name: "tam.nguyen" }).querySelector("img")).toBeNull(),
        );
    });

    it("báo lỗi bằng toast khi tải ảnh thất bại", async () => {
        server.use(
            http.put("*/api/admin/users/tam.nguyen/avatar", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        const dialog = await openEditDialog("tam.nguyen");

        chooseAvatar(dialog, pngFile());
        await dialog.findByRole("img", { name: PREVIEW_ALT });
        fireEvent.click(dialog.getByRole("button", { name: "Lưu thay đổi" }));

        expect(
            await screen.findByText("Chưa cập nhật được tam.nguyen. Đã có lỗi xảy ra"),
        ).toBeInTheDocument();
    });
});

describe("ảnh đại diện trong modal chi tiết", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("hiện ảnh và mở ảnh lớn để xem", async () => {
        await uploadAvatarFor("viet.luu");
        await renderUsers("?user=viet.luu");
        const detail = within(await screen.findByRole("dialog", { name: "Chi tiết người dùng" }));

        fireEvent.click(await detail.findByRole("button", { name: "Xem ảnh đại diện" }));

        const preview = within(
            await screen.findByRole("dialog", { name: "Ảnh đại diện của viet.luu" }),
        );

        expect(preview.getByRole("img", { name: "Ảnh đại diện của viet.luu" })).toHaveAttribute(
            "src",
            PNG_DATA_URL,
        );
    });

    it("hiện chữ cái đầu và không có nút xem ảnh khi chưa có ảnh", async () => {
        await renderUsers("?user=viet.luu");
        const detail = within(await screen.findByRole("dialog", { name: "Chi tiết người dùng" }));
        await detail.findByText("Lưu Quốc Việt");

        expect(detail.queryByRole("button", { name: "Xem ảnh đại diện" })).not.toBeInTheDocument();
    });
});
