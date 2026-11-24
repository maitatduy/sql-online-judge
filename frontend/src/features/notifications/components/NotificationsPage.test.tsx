import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { resetNotifications } from "@/mocks/notifications";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { NotificationsPage } from "./NotificationsPage";

vi.mock("next/navigation", () => ({
    usePathname: () => "/notifications",
}));

describe("trang thông báo", () => {
    beforeEach(() => resetNotifications());

    it("liệt kê thông báo, đánh dấu chưa đọc và cho đọc tất cả", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<NotificationsPage />);

        const list = await screen.findByRole("list", { name: "Thông báo" });
        expect(within(list).getAllByText("Chưa đọc")).toHaveLength(2);
        expect(within(list).getByText("Rating của bạn tăng 24, hiện là 1.536")).toBeVisible();

        fireEvent.click(await screen.findByRole("button", { name: "Đánh dấu tất cả đã đọc" }));

        await waitFor(() => expect(screen.queryByText("Chưa đọc")).toBeNull());
        expect(screen.getAllByRole("listitem")).toHaveLength(3);
    });

    it("bật tắt email cho thông báo quan trọng", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<NotificationsPage />);

        const toggle = await screen.findByRole("switch", { name: /Gửi email khi bài được duyệt/ });
        expect(toggle).toBeChecked();

        fireEvent.click(toggle);

        await waitFor(() => expect(toggle).not.toBeChecked());
    });

    it("khách được mời đăng nhập", async () => {
        renderWithProviders(<NotificationsPage />);

        expect(await screen.findByText("Đăng nhập để xem thông báo")).toBeVisible();
    });
});
