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

function rowOf(table: ReturnType<typeof within>, title: string) {
    return within(table.getByRole("row", { name: new RegExp(title) }));
}

describe("thao tác trên từng bài trong kho", () => {
    beforeEach(() => {
        sessionStorage.clear();
    });

    it("sửa nhanh tên bài thì bảng hiện tên mới", async () => {
        const table = await renderBank();

        fireEvent.click(screen.getByRole("button", { name: "Sửa nhanh bài 41" }));
        const dialog = within(await screen.findByRole("dialog"));
        fireEvent.change(dialog.getByLabelText("Tên bài"), {
            target: { value: "Cặp sinh viên cùng khoá" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Lưu" }));

        expect(await table.findByText("Cặp sinh viên cùng khoá")).toBeInTheDocument();
        expect(await screen.findByText("Đã lưu thay đổi.")).toBeInTheDocument();
    });

    it("ẩn bài thì nhãn đổi thành Đang ẩn, bài trong contest thì báo lỗi", async () => {
        const table = await renderBank();

        fireEvent.click(screen.getByRole("button", { name: "Ẩn bài 41" }));
        fireEvent.click(
            within(await screen.findByRole("dialog")).getByRole("button", { name: "Ẩn bài" }),
        );
        await waitFor(() =>
            expect(rowOf(table, "Cặp sinh viên").getByText("Đang ẩn")).toBeInTheDocument(),
        );

        fireEvent.click(screen.getByRole("button", { name: "Ẩn bài 117" }));
        fireEvent.click(
            within(await screen.findByRole("dialog")).getByRole("button", { name: "Ẩn bài" }),
        );
        expect(
            await screen.findByText("Bài đang thuộc một contest chưa kết thúc"),
        ).toBeInTheDocument();
    });

    it("xoá bài thì bài biến khỏi bảng", async () => {
        const table = await renderBank();

        fireEvent.click(screen.getByRole("button", { name: "Xoá bài 18" }));
        fireEvent.click(
            within(await screen.findByRole("dialog")).getByRole("button", { name: "Xoá bài" }),
        );

        await waitFor(() =>
            expect(table.queryByText("Nhân viên lương cao hơn quản lý")).toBeNull(),
        );
    });

    it("chuyển cho người không phải người ra đề thì báo lỗi ngay ở ô nhập", async () => {
        await renderBank();

        fireEvent.click(screen.getByRole("button", { name: "Chuyển chủ sở hữu bài 41" }));
        const dialog = within(await screen.findByRole("dialog"));
        fireEvent.change(dialog.getByLabelText("Username người nhận"), {
            target: { value: ADMIN_SESSION_USER.username },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Chuyển" }));

        expect(
            await dialog.findByText("Người nhận phải là người ra đề đang hoạt động"),
        ).toBeInTheDocument();
    });

    it("chấm lại một bài thì báo số bài nộp được đưa vào hàng đợi", async () => {
        await renderBank();

        fireEvent.click(screen.getByRole("button", { name: "Chấm lại bài 41" }));
        fireEvent.click(
            within(await screen.findByRole("dialog")).getByRole("button", { name: "Chấm lại" }),
        );

        expect(await screen.findByText("Đang chấm lại 21.385 bài nộp.")).toBeInTheDocument();
    });
});
