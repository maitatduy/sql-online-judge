import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { AdminProblemBank } from "./AdminProblemBank";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

function renderBank(search = "") {
    visit(search ? `/admin/problems?${search}` : "/admin/problems");
    writeMockSession(ADMIN_SESSION_USER);
    return renderWithProviders(
        <ToastProvider>
            <AdminProblemBank />
        </ToastProvider>,
    );
}

async function findTable() {
    return within(await screen.findByRole("table", { name: "Kho bài tập" }));
}

describe("AdminProblemBank", () => {
    beforeEach(() => {
        sessionStorage.clear();
    });

    it("liệt kê bài đã xuất bản kèm người ra đề, chủ đề, phạm vi và số liệu nộp bài", async () => {
        renderBank();

        const table = await findTable();
        const row = within(table.getByRole("row", { name: /Cặp sinh viên cùng lớp/ }));
        expect(row.getByText("41")).toBeInTheDocument();
        expect(row.getByText("an.bui")).toBeInTheDocument();
        expect(row.getByText("JOIN")).toBeInTheDocument();
        expect(row.getByText("Công khai")).toBeInTheDocument();
        expect(row.getByText("21.385")).toBeInTheDocument();
        expect(row.getByText("44,1%")).toBeInTheDocument();
        expect(table.queryByText("Tỉ lệ giữ chân theo cohort tháng")).toBeNull();
        expect(await screen.findByRole("button", { name: /Công khai\s*3/ })).toBeInTheDocument();
        expect(screen.getByText("Tỉ lệ AC thấp")).toBeInTheDocument();
    });

    it("bấm thẻ Chỉ trong contest thì chỉ còn bài chỉ trong contest", async () => {
        renderBank();
        await findTable();

        fireEvent.click(await screen.findByRole("button", { name: /Chỉ trong contest\s*2/ }));

        const table = await findTable();
        await waitFor(() => expect(table.queryByText("Cặp sinh viên cùng lớp")).toBeNull());
        expect(table.getByText("Khoảng trống trong lịch đặt phòng")).toBeInTheDocument();
        expect(window.location.search).toBe("?visibility=CONTEST_ONLY");
    });

    it("không có bài khớp bộ lọc thì báo trống và cho xoá lọc", async () => {
        renderBank("q=khong-co-bai-nay");

        expect(await screen.findByText("Không có bài khớp bộ lọc")).toBeInTheDocument();
        fireEvent.click(screen.getAllByRole("button", { name: "Xoá lọc" }).at(-1)!);

        expect(await screen.findByRole("table", { name: "Kho bài tập" })).toBeInTheDocument();
    });

    it("lỗi khi tải thì hiện trạng thái lỗi có nút Thử lại", async () => {
        server.use(
            http.get("*/api/admin/problems", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        renderBank();

        expect(await screen.findByText("Chưa tải được kho bài tập")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });
});
