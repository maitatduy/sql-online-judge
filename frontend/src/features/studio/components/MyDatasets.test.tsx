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

const TABLE_NAME = "Dataset của tôi";

function renderMyDatasets() {
    visit("/studio/datasets");
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

    it("liệt kê dataset mới sửa nhất trước với mô tả, phiên bản và lúc cập nhật", async () => {
        renderMyDatasets();
        const table = await datasetTable();

        expect(screen.getByText("3 dataset")).toBeInTheDocument();
        expect(namesIn(table)).toEqual(["Công ty", "Trường học", "Cửa hàng trực tuyến"]);
        const company = within(table.getByRole("row", { name: /Công ty/ }));
        expect(company.getByText("Phòng ban và nhân viên kèm lương")).toBeInTheDocument();
        expect(company.getByText("v2 · Nháp")).toBeInTheDocument();
        const school = within(table.getByRole("row", { name: /Trường học/ }));
        expect(school.getByText("v1")).toBeInTheDocument();
        expect(school.getByText("2 ngày trước")).toBeInTheDocument();
    });

    it("tìm theo tên không phân biệt dấu và ghi từ khoá lên URL", async () => {
        renderMyDatasets();
        await datasetTable();

        fireEvent.change(screen.getByRole("searchbox", { name: "Tìm dataset" }), {
            target: { value: "truong" },
        });

        const table = await datasetTable();
        await vi.waitFor(() => expect(namesIn(table)).toEqual(["Trường học"]));
        expect(window.location.search).toBe("?q=truong");
    });

    it("báo khi chưa có dataset nào", async () => {
        writeMockSession(ADMIN_SESSION_USER);
        renderMyDatasets();

        expect(await screen.findByText("Bạn chưa có dataset nào.")).toBeInTheDocument();
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
        expect(alert).toHaveTextContent("Không tải được danh sách dataset");
        expect(within(alert).getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    });

    it("tạo dataset mới, báo thành công và đưa lên đầu danh sách", async () => {
        renderMyDatasets();
        await datasetTable();

        fireEvent.click(screen.getByRole("button", { name: "Tạo dataset" }));
        const dialog = within(await screen.findByRole("dialog", { name: "Tạo dataset" }));
        fireEvent.change(dialog.getByRole("textbox", { name: "Tên dataset" }), {
            target: { value: "Thư viện" },
        });
        fireEvent.change(dialog.getByRole("textbox", { name: "Mô tả" }), {
            target: { value: "Sách, độc giả và lượt mượn" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Tạo dataset" }));

        expect(await screen.findByText("Đã tạo dataset Thư viện.")).toBeInTheDocument();
        expect(screen.queryByRole("dialog", { name: "Tạo dataset" })).not.toBeInTheDocument();
        const table = await datasetTable();
        await vi.waitFor(() => expect(namesIn(table)[0]).toBe("Thư viện"));
        expect(screen.getByText("4 dataset")).toBeInTheDocument();
    });

    it("báo lỗi ngay khi tên dataset trống", async () => {
        renderMyDatasets();
        await datasetTable();

        fireEvent.click(screen.getByRole("button", { name: "Tạo dataset" }));
        const dialog = within(await screen.findByRole("dialog", { name: "Tạo dataset" }));
        fireEvent.click(dialog.getByRole("button", { name: "Tạo dataset" }));

        expect(await dialog.findByText("Không được để trống")).toBeInTheDocument();
    });
});
