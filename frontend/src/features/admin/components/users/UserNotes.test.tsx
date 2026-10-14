import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { apiClient } from "@/lib/api-client";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminUserPage } from "./AdminUserPage";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

async function renderNotes(username: string) {
    visit(`/admin/users/${username}`);
    writeMockSession(ADMIN_SESSION_USER);
    renderWithProviders(
        <ToastProvider>
            <AdminUserPage username={username} />
        </ToastProvider>,
    );
    return within(await screen.findByRole("region", { name: "Ghi chú nội bộ" }));
}

const noteField = (notes: ReturnType<typeof within>) =>
    notes.getByRole("textbox", { name: "Thêm ghi chú" });

describe("ghi chú nội bộ trên trang chi tiết", () => {
    beforeEach(() => {
        visit("/admin/users");
    });

    it("hiện ghi chú kèm người viết và ngày", async () => {
        const notes = await renderNotes("viet.luu");

        expect(
            await notes.findByText("Đã nhắc nhở qua tin nhắn, người dùng hứa không tái phạm."),
        ).toBeInTheDocument();
        expect(notes.getByText("mod.thu · 12/09/2026")).toBeInTheDocument();
    });

    it("thêm ghi chú mới lên đầu danh sách rồi xoá trống ô nhập", async () => {
        const notes = await renderNotes("viet.luu");
        await notes.findByText("mod.thu · 12/09/2026");

        expect(notes.getByRole("button", { name: "Lưu ghi chú" })).toBeDisabled();
        expect(notes.getByText("Chỉ quản trị viên thấy ghi chú này.")).toBeInTheDocument();

        fireEvent.change(noteField(notes), { target: { value: "Theo dõi Weekly #38" } });
        fireEvent.click(notes.getByRole("button", { name: "Lưu ghi chú" }));

        expect(await screen.findByText("Đã thêm ghi chú.")).toBeInTheDocument();
        await waitFor(() =>
            expect(notes.getAllByRole("listitem")[0]).toHaveTextContent("Theo dõi Weekly #38"),
        );
        expect(noteField(notes)).toHaveValue("");
    });

    it("báo lỗi khi ghi chú quá dài", async () => {
        const notes = await renderNotes("viet.luu");
        await notes.findByText("mod.thu · 12/09/2026");

        fireEvent.change(noteField(notes), { target: { value: "a".repeat(1001) } });
        fireEvent.click(notes.getByRole("button", { name: "Lưu ghi chú" }));

        expect(await notes.findByText("Ghi chú tối đa 1.000 ký tự")).toBeInTheDocument();
    });

    it("báo chưa có ghi chú", async () => {
        const notes = await renderNotes("tam.nguyen");

        expect(await notes.findByText("Chưa có ghi chú nào.")).toBeInTheDocument();
    });

    it("không cho thêm ghi chú với tài khoản đã xoá", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        await apiClient.delete("/api/admin/users/viet.luu", { reason: "Tạo nhầm" });
        const notes = await renderNotes("viet.luu");

        await notes.findByText("mod.thu · 12/09/2026");

        expect(notes.queryByRole("textbox", { name: "Thêm ghi chú" })).not.toBeInTheDocument();
    });
});
