import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminProblemBank } from "./AdminProblemBank";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function renderBank() {
    visit("/admin/problems");
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminProblemBank />
        </ToastProvider>,
    );
    return within(await screen.findByRole("table", { name: "Kho bài tập" }));
}

describe("thao tác hàng loạt trong kho", () => {
    beforeEach(() => {
        sessionStorage.clear();
    });

    it("đổi độ khó hàng loạt rồi báo số bài cập nhật và bỏ chọn", async () => {
        const table = await renderBank();

        fireEvent.click(screen.getByRole("checkbox", { name: "Chọn bài 41" }));
        fireEvent.click(screen.getByRole("checkbox", { name: "Chọn bài 18" }));
        const bar = within(screen.getByRole("region", { name: "Thao tác hàng loạt" }));
        expect(bar.getByText("Đã chọn 2")).toBeInTheDocument();
        fireEvent.click(bar.getByRole("button", { name: "Đổi độ khó" }));
        const dialog = within(await screen.findByRole("dialog"));
        fireEvent.change(dialog.getByLabelText("Độ khó"), { target: { value: "HARD" } });
        fireEvent.click(dialog.getByRole("button", { name: "Áp dụng" }));

        expect(await screen.findByText("Đã cập nhật 2 bài.")).toBeInTheDocument();
        await waitFor(() =>
            expect(
                within(table.getByRole("row", { name: /Cặp sinh viên/ })).getByText("Khó"),
            ).toBeInTheDocument(),
        );
        expect(screen.queryByRole("region", { name: "Thao tác hàng loạt" })).toBeNull();
    });

    it("ẩn hàng loạt báo cả bài lỗi khi có bài trong contest", async () => {
        await renderBank();

        fireEvent.click(screen.getByRole("checkbox", { name: "Chọn bài 41" }));
        fireEvent.click(screen.getByRole("checkbox", { name: "Chọn bài 117" }));
        fireEvent.click(screen.getByRole("button", { name: "Ẩn bài" }));
        fireEvent.click(
            within(await screen.findByRole("dialog")).getByRole("button", { name: "Ẩn bài" }),
        );

        expect(await screen.findByText("Đã cập nhật 1 bài, 1 bài lỗi.")).toBeInTheDocument();
    });
});
