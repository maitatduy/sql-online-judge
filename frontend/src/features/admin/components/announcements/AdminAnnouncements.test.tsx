import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetAnnouncements } from "@/mocks/admin-announcements";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { AdminAnnouncements } from "./AdminAnnouncements";

describe("màn thông báo hệ thống", () => {
    beforeEach(() => {
        resetAnnouncements();
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("gửi thông báo mới và thấy nó đứng đầu lịch sử", async () => {
        renderWithProviders(
            <ToastProvider>
                <AdminAnnouncements />
            </ToastProvider>,
        );
        const history = await screen.findByRole("region", { name: "Đã gửi" });
        expect(
            await within(history).findByText("Contest SQL Cup tháng 12 mở đăng ký"),
        ).toBeVisible();

        fireEvent.change(screen.getByLabelText("Nội dung"), {
            target: { value: "Máy chấm bảo trì lúc 22 giờ" },
        });
        fireEvent.change(screen.getByLabelText("Đường dẫn (không bắt buộc)"), {
            target: { value: "/contests" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Gửi tới mọi người dùng" }));

        expect(
            await screen.findByText("Đã bắt đầu gửi thông báo tới mọi người dùng."),
        ).toBeVisible();
        await within(history).findByText("Máy chấm bảo trì lúc 22 giờ");
        const items = within(history).getAllByRole("listitem");
        expect(items[0]).toHaveTextContent("Máy chấm bảo trì lúc 22 giờ");
        expect(items[0]).toHaveTextContent("2.431 người nhận");
        expect(screen.getByLabelText("Nội dung")).toHaveValue("");
    });
});
