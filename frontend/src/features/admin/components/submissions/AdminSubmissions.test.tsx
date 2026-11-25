import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminSubmissions } from "./AdminSubmissions";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

function rowsOf(table: HTMLElement): HTMLElement[] {
    return within(table).getAllByRole("row").slice(1);
}

describe("trang bài nộp của admin", () => {
    beforeEach(() => {
        writeMockSession(ADMIN_SESSION_USER);
        visit("/admin/submissions");
    });

    it("báo đang tải rồi hiện trang đầu, mới nhất trước, kèm phân trang", async () => {
        renderWithProviders(<AdminSubmissions />);

        expect(screen.getByRole("status", { name: "Đang tải bài nộp" })).toBeInTheDocument();
        const table = await screen.findByRole("table", { name: "Bài nộp" });

        expect(rowsOf(table)).toHaveLength(20);
        expect(rowsOf(table)[0]).toHaveTextContent("minh.anh");
        expect(rowsOf(table)[0]).toHaveTextContent("184. Lương cao nhất mỗi phòng ban");
        expect(screen.getByRole("navigation", { name: "Phân trang" })).toBeInTheDocument();
    });

    it("lọc theo kết quả và nguồn, ghi bộ lọc lên URL", async () => {
        visit("/admin/submissions?result=RE");
        renderWithProviders(<AdminSubmissions />);

        const table = await screen.findByRole("table", { name: "Bài nộp" });

        expect(rowsOf(table).length).toBeGreaterThan(0);
        rowsOf(table).forEach((row) => expect(row).toHaveTextContent("RE"));
        expect(screen.getByRole("combobox", { name: "Lọc theo kết quả" })).toHaveValue("RE");

        fireEvent.change(screen.getByRole("combobox", { name: "Lọc theo nguồn" }), {
            target: { value: "CONTEST" },
        });

        expect(window.location.search).toContain("source=CONTEST");
    });

    it("không có bài khớp bộ lọc thì mời xoá lọc", async () => {
        visit("/admin/submissions?username=khong.co");
        renderWithProviders(<AdminSubmissions />);

        const empty = (await screen.findByText("Không có bài nộp khớp bộ lọc")).parentElement;
        fireEvent.click(within(empty as HTMLElement).getByRole("button", { name: "Xoá lọc" }));

        await waitFor(() => expect(window.location.search).toBe(""));
    });

    it("mở chi tiết thấy code, lỗi và số testcase", async () => {
        visit("/admin/submissions?result=RE");
        renderWithProviders(<AdminSubmissions />);

        const table = await screen.findByRole("table", { name: "Bài nộp" });
        fireEvent.click(within(rowsOf(table)[0]).getByRole("button", { name: /^Xem bài nộp/ }));

        const dialog = await screen.findByRole("dialog");
        expect(await within(dialog).findByLabelText("Code")).toHaveTextContent("GROUP BY");
        expect(within(dialog).getByText('column "salry" does not exist')).toBeInTheDocument();
        expect(within(dialog).getByText("3/3")).toBeInTheDocument();
    });

    it("báo lỗi kèm nút thử lại khi không có quyền", async () => {
        writeMockSession(DEMO_SESSION_USER);
        renderWithProviders(<AdminSubmissions />);

        const alert = await screen.findByRole("alert");

        expect(alert).toHaveTextContent("Chưa tải được bài nộp");
        expect(within(alert).getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });
});
