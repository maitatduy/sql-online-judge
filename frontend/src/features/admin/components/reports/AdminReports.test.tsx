import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetAdminReports } from "@/mocks/admin-reports";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminReports } from "./AdminReports";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

function renderReports() {
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminReports />
        </ToastProvider>,
    );
}

describe("trang báo cáo vi phạm", () => {
    beforeEach(() => {
        resetAdminReports();
        visit("/admin/reports");
    });

    it("chọn một báo cáo thì thấy nội dung, người báo cáo và số vi phạm trước", async () => {
        renderReports();

        const list = await screen.findByRole("region", { name: "Danh sách báo cáo" });
        fireEvent.click(await within(list).findByRole("button", { name: /Đáp án bài B/ }));

        expect(await screen.findByText("Báo cáo: 3")).toBeVisible();
        const detail = screen.getByRole("region", { name: "Chi tiết báo cáo" });
        expect(within(detail).getByText(/Vi phạm trước/)).toBeVisible();
        expect(within(detail).getByText("Đăng lời giải bài B khi contest đang chạy")).toBeVisible();
        expect(await within(detail).findByRole("button", { name: "Khoá tác giả" })).toBeVisible();
    });

    it("gỡ nội dung thì báo cáo rời khỏi danh sách chờ xử lý", async () => {
        renderReports();

        const list = await screen.findByRole("region", { name: "Danh sách báo cáo" });
        fireEvent.click(await within(list).findByRole("button", { name: /Mua khoá học/ }));
        fireEvent.click(await screen.findByRole("button", { name: "Gỡ nội dung" }));

        expect(await screen.findByText("Đã xử lý vi phạm.")).toBeVisible();
        expect(await screen.findByText("Chọn một báo cáo để xem.")).toBeVisible();
        expect(within(list).queryByRole("button", { name: /Mua khoá học/ })).toBeNull();
        fireEvent.click(screen.getByRole("button", { name: "Đã gỡ" }));
        expect(await within(list).findByRole("button", { name: /Mua khoá học/ })).toBeVisible();
    });
});
