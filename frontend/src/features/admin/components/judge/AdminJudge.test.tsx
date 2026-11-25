import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { resetAdminJudge } from "@/mocks/admin-judge";
import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { AdminJudge } from "./AdminJudge";

function renderJudge() {
    renderWithProviders(
        <ToastProvider>
            <AdminJudge />
        </ToastProvider>,
    );
}

function tile(label: string): HTMLElement {
    return screen.getByRole("group", { name: label });
}

describe("trang máy chấm", () => {
    beforeEach(() => {
        resetAdminJudge();
        writeMockSession(ADMIN_SESSION_USER);
    });

    it("báo đang tải rồi hiện số liệu hàng đợi", async () => {
        renderJudge();

        expect(screen.getByRole("status", { name: "Đang tải hàng đợi" })).toBeInTheDocument();
        expect(await screen.findByRole("group", { name: "Đang chờ chấm" })).toHaveTextContent("4");
        expect(tile("Bị kẹt")).toHaveTextContent("Chờ quá 2 phút");
        expect(tile("Đã chấm")).toHaveTextContent("318");
    });

    it("liệt kê bài kẹt và bài SE, chấm lại thì bài rời danh sách và báo thành công", async () => {
        renderJudge();

        const table = await screen.findByRole("table", { name: "Bài cần xử lý" });
        const rows = within(table).getAllByRole("row").slice(1);
        expect(rows.length).toBeGreaterThan(1);
        expect(within(table).getAllByText("Bị kẹt").length).toBeGreaterThan(0);
        expect(within(table).getAllByText("SE").length).toBeGreaterThan(0);

        fireEvent.click(within(rows[0]).getByRole("button", { name: /^Chấm lại/ }));

        expect(await screen.findByText("Đã đưa bài nộp vào hàng đợi chấm lại")).toBeInTheDocument();
        await waitFor(() =>
            expect(within(table).getAllByRole("row").slice(1)).toHaveLength(rows.length - 1),
        );
    });

    it("mở chi tiết bài cần xử lý thấy code", async () => {
        renderJudge();

        const table = await screen.findByRole("table", { name: "Bài cần xử lý" });
        fireEvent.click(within(table).getAllByRole("button", { name: /^Xem bài nộp/ })[0]);

        const dialog = await screen.findByRole("dialog");
        expect(await within(dialog).findByLabelText("Code")).toHaveTextContent("SELECT");
    });

    it("báo lỗi khi không có quyền", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderJudge();

        expect(await screen.findByText("Chưa tải được hàng đợi")).toBeInTheDocument();
        expect(screen.getByText("Chưa tải được bài cần xử lý")).toBeInTheDocument();
    });
});
