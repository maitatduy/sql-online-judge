import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { MyProblems } from "./MyProblems";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const TABLE_NAME = "Bài của tôi";

async function renderMyProblems(search = "") {
    visit(`/studio/problems${search}`);
    renderWithProviders(
        <ToastProvider>
            <MyProblems />
        </ToastProvider>,
    );
    return within(await screen.findByRole("table", { name: TABLE_NAME }));
}

function titlesIn(table: ReturnType<typeof within>) {
    return table
        .getAllByRole("row")
        .slice(1)
        .map((row: HTMLElement) => within(row).getAllByRole("cell")[0].textContent);
}

describe("MyProblems", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("hiện tổng số bài, số đã xuất bản và số bài của từng trạng thái trong ô lọc", async () => {
        await renderMyProblems();

        expect(await screen.findByText("9 bài · 5 đã xuất bản")).toBeInTheDocument();
        const statusSelect = screen.getByRole("combobox", { name: "Trạng thái" });
        expect(statusSelect).toHaveValue("");
        expect(
            within(statusSelect)
                .getAllByRole("option")
                .map((option) => option.textContent),
        ).toEqual([
            "Mọi trạng thái · 9",
            "Nháp · 2",
            "Chờ duyệt · 1",
            "Cần sửa · 1",
            "Đã xuất bản · 5",
        ]);
        expect(screen.queryByRole("tablist")).not.toBeInTheDocument();
    });

    it("liệt kê bài mới sửa nhất trước với trạng thái, độ khó và lúc cập nhật", async () => {
        const table = await renderMyProblems();

        expect(titlesIn(table)).toHaveLength(9);
        expect(titlesIn(table)[0]).toBe("Phễu chuyển đổi 4 bước");
        const needsFix = within(table.getByRole("row", { name: /Chuỗi ngày hoạt động dài nhất/ }));
        expect(needsFix.getByText("Cần sửa")).toBeInTheDocument();
        expect(needsFix.getByText("Khó")).toHaveClass("text-difficulty-hard");
        expect(needsFix.getByText("1 ngày trước")).toBeInTheDocument();
        expect(
            needsFix.getByRole("link", { name: "Chuỗi ngày hoạt động dài nhất" }),
        ).toHaveAttribute("href", expect.stringMatching(/^\/studio\/problems\/0192f3d0-/));
    });

    it("chọn trạng thái Nháp thì chỉ còn bài nháp và ghi trạng thái lên URL", async () => {
        await renderMyProblems();

        fireEvent.change(screen.getByRole("combobox", { name: "Trạng thái" }), {
            target: { value: "DRAFT" },
        });

        const table = within(await screen.findByRole("table", { name: TABLE_NAME }));
        await vi.waitFor(() =>
            expect(titlesIn(table)).toEqual([
                "Phễu chuyển đổi 4 bước",
                "Lương trung vị mỗi phòng ban",
            ]),
        );
        expect(window.location.search).toBe("?status=DRAFT");
        expect(screen.getByRole("combobox", { name: "Trạng thái" })).toHaveValue("DRAFT");
    });

    it("tìm theo tên không phân biệt dấu", async () => {
        await renderMyProblems();

        fireEvent.change(screen.getByRole("searchbox", { name: "Tìm bài" }), {
            target: { value: "trung vi" },
        });

        const table = within(await screen.findByRole("table", { name: TABLE_NAME }));
        await vi.waitFor(() => expect(titlesIn(table)).toEqual(["Lương trung vị mỗi phòng ban"]));
    });

    it("báo khi chưa có bài nào", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        visit("/studio/problems");
        renderWithProviders(
            <ToastProvider>
                <MyProblems />
            </ToastProvider>,
        );

        expect(await screen.findByText("Bạn chưa có bài nào.")).toBeInTheDocument();
    });

    it("hiện lỗi có nút thử lại khi không tải được danh sách", async () => {
        server.use(
            http.get("*/api/studio/problems", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        visit("/studio/problems");
        renderWithProviders(
            <ToastProvider>
                <MyProblems />
            </ToastProvider>,
        );

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveTextContent("Không tải được danh sách bài");
        expect(within(alert).getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });
});
