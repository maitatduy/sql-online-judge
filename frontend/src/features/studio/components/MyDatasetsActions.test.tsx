import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";
import { visit } from "@/test/url-search-params";

import { MyDatasets } from "./MyDatasets";

vi.mock("next/navigation", async () => ({
    useSearchParams: (await import("@/test/url-search-params")).useUrlSearchParams,
}));

const TABLE_NAME = "Database của tôi";

async function renderMyDatasets() {
    visit("/studio/databases");
    renderWithProviders(
        <ToastProvider>
            <MyDatasets />
        </ToastProvider>,
    );
    return within(await screen.findByRole("table", { name: TABLE_NAME }));
}

function namesIn(table: ReturnType<typeof within>) {
    return table
        .getAllByRole("row")
        .slice(1)
        .map((row: HTMLElement) => within(row).getAllByRole("cell")[0].textContent);
}

function rowOf(table: ReturnType<typeof within>, name: string) {
    return within(table.getByRole("row", { name: new RegExp(name) }));
}

describe("MyDatasets - bộ lọc, phân trang và hành động", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("không hiện phiên bản ở bảng hay bộ lọc", async () => {
        const table = await renderMyDatasets();

        expect(table.getAllByRole("columnheader").map((header) => header.textContent)).toEqual([
            "Tên",
            "Mô tả",
            "Cập nhật",
            "Hành động",
        ]);
        expect(screen.queryByRole("combobox", { name: /phiên bản/i })).not.toBeInTheDocument();
    });

    it("sắp xếp theo tên A-Z", async () => {
        await renderMyDatasets();

        fireEvent.change(screen.getByRole("combobox", { name: "Sắp xếp" }), {
            target: { value: "name,asc" },
        });

        const table = within(await screen.findByRole("table", { name: TABLE_NAME }));
        await vi.waitFor(() =>
            expect(namesIn(table)).toEqual(["Công ty", "Cửa hàng trực tuyến", "Trường học"]),
        );
    });

    it("luôn hiện thanh phân trang và đổi được số dòng mỗi trang", async () => {
        await renderMyDatasets();

        expect(screen.getByText("1-3 trong 3 database")).toBeInTheDocument();
        fireEvent.change(screen.getByRole("combobox", { name: "Số dòng mỗi trang" }), {
            target: { value: "50" },
        });

        await vi.waitFor(() => expect(window.location.search).toBe("?size=50"));
    });

    it("xem chi tiết database gồm bảng, cột và bộ dữ liệu", async () => {
        const table = await renderMyDatasets();

        fireEvent.click(
            rowOf(table, "Công ty").getByRole("button", { name: "Xem chi tiết Công ty" }),
        );

        const dialog = within(await screen.findByRole("dialog", { name: "Công ty" }));
        expect(await dialog.findByRole("heading", { name: "employees" })).toBeInTheDocument();
        expect(dialog.getByText("department_id")).toBeInTheDocument();
        expect(dialog.getByText("Mẫu · Công khai · 9 dòng")).toBeInTheDocument();
        expect(dialog.getByRole("link", { name: "Sửa database" })).toHaveAttribute(
            "href",
            "/studio/databases/0192f3e0-0000-7000-8000-000000000001",
        );
    });

    it("nút sửa mở trang soạn database", async () => {
        const table = await renderMyDatasets();

        expect(
            rowOf(table, "Trường học").getByRole("link", { name: "Sửa Trường học" }),
        ).toHaveAttribute("href", "/studio/databases/0192f3e0-0000-7000-8000-000000000002");
    });

    it("xoá database chưa bài nào dùng sau khi xác nhận", async () => {
        const table = await renderMyDatasets();

        fireEvent.click(rowOf(table, "Công ty").getByRole("button", { name: "Xoá Công ty" }));
        const dialog = within(await screen.findByRole("alertdialog"));
        expect(dialog.getByText("Xoá database Công ty?")).toBeInTheDocument();
        fireEvent.click(dialog.getByRole("button", { name: "Xoá database" }));

        expect(await screen.findByText("Đã xoá database Công ty.")).toBeInTheDocument();
        await vi.waitFor(() =>
            expect(namesIn(table)).toEqual(["Trường học", "Cửa hàng trực tuyến"]),
        );
    });

    it("báo rõ lý do khi database đang được bài dùng", async () => {
        const table = await renderMyDatasets();

        fireEvent.click(rowOf(table, "Trường học").getByRole("button", { name: "Xoá Trường học" }));
        fireEvent.click(
            within(await screen.findByRole("alertdialog")).getByRole("button", {
                name: "Xoá database",
            }),
        );

        expect(
            await screen.findByText(
                "Chưa xoá được database. Database đang được 2 bài dùng nên chưa xoá được",
            ),
        ).toBeInTheDocument();
        expect(namesIn(table)).toContain("Trường học");
    });
});
