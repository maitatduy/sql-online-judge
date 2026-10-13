import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AdminSidebarDrawer } from "./AdminSidebarDrawer";

const ADMIN_USER = { username: "admin.huy", displayName: "Admin Huy" };

function drawer(activePath: string) {
    return <AdminSidebarDrawer activePath={activePath} user={ADMIN_USER} />;
}

function openDrawer() {
    const menuButton = screen.getByRole("button", { name: "Mở menu quản trị" });
    menuButton.focus();
    fireEvent.click(menuButton);
    return { menuButton, dialog: screen.getByRole("dialog", { name: "Menu quản trị" }) };
}

describe("AdminSidebarDrawer", () => {
    it("ẩn menu cho tới khi bấm nút mở", () => {
        render(drawer("/admin"));

        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
        expect(screen.queryByRole("navigation", { name: "Quản trị" })).not.toBeInTheDocument();
    });

    it("thanh trên cùng không hiện chữ Admin Console", () => {
        render(drawer("/admin"));

        expect(screen.queryByText("Admin Console")).not.toBeInTheDocument();
    });

    it("mở menu quản trị đầy đủ trong ngăn kéo", () => {
        render(drawer("/admin/users"));

        const { dialog } = openDrawer();
        const navigation = within(dialog).getByRole("navigation", { name: "Quản trị" });

        expect(within(navigation).getByRole("link", { name: /^Người dùng/ })).toHaveAttribute(
            "aria-current",
            "page",
        );
        expect(within(navigation).getByRole("link", { name: /^Cấu hình/ })).toBeInTheDocument();
    });

    it("đóng bằng phím Esc và trả focus về nút mở", async () => {
        render(drawer("/admin"));
        const { menuButton, dialog } = openDrawer();

        fireEvent.keyDown(dialog, { key: "Escape" });

        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
        await waitFor(() => expect(menuButton).toHaveFocus());
    });

    it("đóng bằng nút đóng", async () => {
        render(drawer("/admin"));
        const { dialog } = openDrawer();

        fireEvent.click(within(dialog).getByRole("button", { name: "Đóng menu quản trị" }));

        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    });

    it("tự đóng khi chuyển sang trang khác", async () => {
        const view = render(drawer("/admin"));
        openDrawer();

        view.rerender(drawer("/admin/users"));

        await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    });
});
