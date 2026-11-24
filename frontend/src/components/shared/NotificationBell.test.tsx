import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { resetNotifications } from "@/mocks/notifications";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { NotificationBell } from "./NotificationBell";

describe("chuông thông báo", () => {
    beforeEach(() => resetNotifications());

    it("khách không thấy chuông", async () => {
        renderWithProviders(<NotificationBell />);

        await waitFor(() => expect(screen.queryByRole("button")).toBeNull());
    });

    it("hiện số chưa đọc, mở danh sách và đánh dấu tất cả đã đọc", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<NotificationBell />);

        const bell = await screen.findByRole("button", { name: "Thông báo, 2 chưa đọc" });
        fireEvent.click(bell);

        const list = await screen.findByRole("list", { name: "Thông báo" });
        expect(within(list).getAllByRole("listitem")).toHaveLength(3);
        expect(within(list).getByRole("link", { name: /Có bình luận mới/ })).toHaveAttribute(
            "href",
            "/community/0192f3a0-0000-7000-9000-000000000002",
        );
        expect(screen.getByRole("link", { name: "Xem tất cả" })).toHaveAttribute(
            "href",
            "/notifications",
        );

        fireEvent.click(screen.getByRole("button", { name: "Đánh dấu đã đọc" }));

        expect(await screen.findByRole("button", { name: "Thông báo" })).toBeInTheDocument();
    });
});
