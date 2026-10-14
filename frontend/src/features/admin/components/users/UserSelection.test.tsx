import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUsers } from "./AdminUsers";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const BAR_NAME = "Thao tác hàng loạt";
const SELECT_PAGE = "Chọn tất cả trên trang";

async function renderLoadedUsers() {
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUsers />
        </ToastProvider>,
    );
    await screen.findByRole("row", { name: "tam.nguyen" });
}

const rowCheckbox = (username: string) =>
    screen.getByRole("checkbox", { name: `Chọn ${username}` });
const pageCheckbox = () => screen.getByRole<HTMLInputElement>("checkbox", { name: SELECT_PAGE });
const queryBar = () => screen.queryByRole("region", { name: BAR_NAME });
const bar = () => within(screen.getByRole("region", { name: BAR_NAME }));

describe("chọn nhiều tài khoản", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("không hiện thanh hàng loạt khi chưa chọn ai", async () => {
        await renderLoadedUsers();

        expect(queryBar()).not.toBeInTheDocument();
        expect(pageCheckbox()).not.toBeChecked();
        expect(rowCheckbox("tam.nguyen")).not.toBeChecked();
    });

    it("chọn một dòng thì hiện số đã chọn mà không mở panel", async () => {
        await renderLoadedUsers();

        fireEvent.click(rowCheckbox("tam.nguyen"));

        expect(bar().getByText("Đã chọn 1")).toBeInTheDocument();
        expect(rowCheckbox("tam.nguyen")).toBeChecked();
        expect(window.location.search).toBe("");
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    it("báo trạng thái chọn một phần ở ô chọn cả trang", async () => {
        await renderLoadedUsers();

        fireEvent.click(rowCheckbox("tam.nguyen"));

        expect(pageCheckbox()).not.toBeChecked();
        expect(pageCheckbox().indeterminate).toBe(true);
    });

    it("chọn cả trang rồi bỏ chọn cả trang", async () => {
        await renderLoadedUsers();

        fireEvent.click(pageCheckbox());

        expect(bar().getByText("Đã chọn 20")).toBeInTheDocument();
        expect(pageCheckbox()).toBeChecked();
        expect(pageCheckbox().indeterminate).toBe(false);
        expect(rowCheckbox("datahust")).toBeChecked();

        fireEvent.click(pageCheckbox());

        expect(queryBar()).not.toBeInTheDocument();
        expect(rowCheckbox("datahust")).not.toBeChecked();
    });

    it("giữ lựa chọn khi đổi bộ lọc và cộng dồn qua các trang kết quả", async () => {
        await renderLoadedUsers();
        fireEvent.click(rowCheckbox("tam.nguyen"));

        fireEvent.change(screen.getByRole("combobox", { name: "Lọc theo vai trò" }), {
            target: { value: "SETTER" },
        });
        await screen.findByText("1-3 trong 3");

        expect(bar().getByText("Đã chọn 1")).toBeInTheDocument();
        expect(pageCheckbox()).not.toBeChecked();

        fireEvent.click(pageCheckbox());

        expect(bar().getByText("Đã chọn 4")).toBeInTheDocument();

        fireEvent.click(pageCheckbox());

        expect(bar().getByText("Đã chọn 1")).toBeInTheDocument();

        fireEvent.click(screen.getAllByRole("button", { name: "Xoá lọc" })[0]);
        await screen.findByText("1-20 trong 62");

        expect(rowCheckbox("tam.nguyen")).toBeChecked();
    });

    it("bỏ chọn tất cả bằng nút Bỏ chọn", async () => {
        await renderLoadedUsers();
        fireEvent.click(pageCheckbox());

        fireEvent.click(bar().getByRole("button", { name: "Bỏ chọn" }));

        await waitFor(() => expect(queryBar()).not.toBeInTheDocument());
        expect(pageCheckbox()).not.toBeChecked();
    });

    it("có liên kết Gửi thông báo tới trang tin nhắn", async () => {
        await renderLoadedUsers();
        fireEvent.click(rowCheckbox("tam.nguyen"));

        expect(bar().getByRole("link", { name: "Gửi thông báo" })).toHaveAttribute(
            "href",
            "/admin/messages",
        );
    });
});
