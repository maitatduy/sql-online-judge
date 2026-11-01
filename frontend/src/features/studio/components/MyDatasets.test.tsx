import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { ADMIN_SESSION_USER, SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { MyDatasets } from "./MyDatasets";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const TABLE_NAME = "Database của tôi";

function renderMyDatasets() {
    visit("/studio/databases");
    renderWithProviders(
        <ToastProvider>
            <MyDatasets />
        </ToastProvider>,
    );
}

async function datasetTable() {
    return within(await screen.findByRole("table", { name: TABLE_NAME }));
}

function namesIn(table: ReturnType<typeof within>) {
    return table
        .getAllByRole("row")
        .slice(1)
        .map((row: HTMLElement) => within(row).getAllByRole("cell")[0].textContent);
}

describe("MyDatasets", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("liệt kê database mới sửa nhất trước với mô tả và lúc cập nhật", async () => {
        renderMyDatasets();
        const table = await datasetTable();

        expect(screen.getByText("3 database")).toBeInTheDocument();
        expect(namesIn(table)).toEqual(["Công ty", "Trường học", "Cửa hàng trực tuyến"]);
        const company = within(table.getByRole("row", { name: /Công ty/ }));
        expect(company.getByText("Phòng ban và nhân viên kèm lương")).toBeInTheDocument();
        expect(company.getByRole("link", { name: "Công ty" })).toHaveAttribute(
            "href",
            "/studio/databases/0192f3e0-0000-7000-8000-000000000001",
        );
        const school = within(table.getByRole("row", { name: /Trường học/ }));
        expect(school.getByText("2 ngày trước")).toBeInTheDocument();
    });

    it("tìm theo tên không phân biệt dấu và ghi từ khoá lên URL", async () => {
        renderMyDatasets();
        await datasetTable();

        fireEvent.change(screen.getByRole("searchbox", { name: "Tìm database" }), {
            target: { value: "truong" },
        });

        const table = await datasetTable();
        await vi.waitFor(() => expect(namesIn(table)).toEqual(["Trường học"]));
        expect(window.location.search).toBe("?q=truong");
    });

    it("báo khi chưa có database nào", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        renderMyDatasets();

        expect(await screen.findByText("Bạn chưa có database nào.")).toBeInTheDocument();
    });

    it("hiện lỗi có nút thử lại khi không tải được danh sách", async () => {
        server.use(
            http.get("*/api/studio/datasets", () =>
                HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                ),
            ),
        );
        renderMyDatasets();

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveTextContent("Không tải được danh sách database");
        expect(within(alert).getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });

    it("tạo database mới, báo thành công và đưa lên đầu danh sách", async () => {
        renderMyDatasets();
        await datasetTable();

        fireEvent.click(screen.getByRole("button", { name: "Tạo database" }));
        const dialog = within(await screen.findByRole("dialog", { name: "Tạo database" }));
        fireEvent.change(dialog.getByRole("textbox", { name: "Tên database" }), {
            target: { value: "Thư viện" },
        });
        fireEvent.change(dialog.getByRole("textbox", { name: "Mô tả" }), {
            target: { value: "Sách, độc giả và lượt mượn" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Tạo database" }));

        expect(await screen.findByText("Đã tạo database Thư viện.")).toBeInTheDocument();
        expect(screen.queryByRole("dialog", { name: "Tạo database" })).not.toBeInTheDocument();
        const table = await datasetTable();
        await vi.waitFor(() => expect(namesIn(table)[0]).toBe("Thư viện"));
        expect(screen.getByText("4 database")).toBeInTheDocument();
    });

    it("báo lỗi ngay khi tên database trống", async () => {
        renderMyDatasets();
        await datasetTable();

        fireEvent.click(screen.getByRole("button", { name: "Tạo database" }));
        const dialog = within(await screen.findByRole("dialog", { name: "Tạo database" }));
        fireEvent.click(dialog.getByRole("button", { name: "Tạo database" }));

        expect(await dialog.findByText("Không được để trống")).toBeInTheDocument();
    });
});
