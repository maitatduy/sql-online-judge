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

const TABLE_NAME = "Dataset của tôi";

async function renderMyDatasets() {
    visit("/studio/datasets");
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

    it("lọc dataset đang có bản nháp và ghi lên URL", async () => {
        await renderMyDatasets();

        fireEvent.change(screen.getByRole("combobox", { name: "Trạng thái phiên bản" }), {
            target: { value: "DRAFT" },
        });

        const table = within(await screen.findByRole("table", { name: TABLE_NAME }));
        await vi.waitFor(() => expect(namesIn(table)).toEqual(["Công ty"]));
        expect(window.location.search).toBe("?status=DRAFT");
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

        expect(screen.getByText("1-3 trong 3 dataset")).toBeInTheDocument();
        fireEvent.change(screen.getByRole("combobox", { name: "Số dòng mỗi trang" }), {
            target: { value: "50" },
        });

        await vi.waitFor(() => expect(window.location.search).toBe("?size=50"));
    });

    it("xem chi tiết dataset gồm phiên bản, bảng và cột", async () => {
        const table = await renderMyDatasets();

        fireEvent.click(
            rowOf(table, "Công ty").getByRole("button", { name: "Xem chi tiết Công ty" }),
        );

        const dialog = within(await screen.findByRole("dialog", { name: "Công ty" }));
        expect(await dialog.findByText("Phiên bản 2 · Nháp")).toBeInTheDocument();
        expect(dialog.getByRole("heading", { name: "employees" })).toBeInTheDocument();
        expect(dialog.getByText("department_id")).toBeInTheDocument();
        expect(dialog.getByText("Chưa có bộ dữ liệu nào.")).toBeInTheDocument();
        expect(dialog.getByRole("link", { name: "Sửa dataset" })).toHaveAttribute(
            "href",
            "/studio/datasets/0192f3e0-0000-7000-8000-000000000001",
        );
    });

    it("nút sửa mở trang soạn dataset", async () => {
        const table = await renderMyDatasets();

        expect(
            rowOf(table, "Trường học").getByRole("link", { name: "Sửa Trường học" }),
        ).toHaveAttribute("href", "/studio/datasets/0192f3e0-0000-7000-8000-000000000002");
    });

    it("xoá dataset chưa bài nào dùng sau khi xác nhận", async () => {
        const table = await renderMyDatasets();

        fireEvent.click(rowOf(table, "Công ty").getByRole("button", { name: "Xoá Công ty" }));
        const dialog = within(await screen.findByRole("alertdialog"));
        expect(dialog.getByText("Xoá dataset Công ty?")).toBeInTheDocument();
        fireEvent.click(dialog.getByRole("button", { name: "Xoá dataset" }));

        expect(await screen.findByText("Đã xoá dataset Công ty.")).toBeInTheDocument();
        await vi.waitFor(() =>
            expect(namesIn(table)).toEqual(["Trường học", "Cửa hàng trực tuyến"]),
        );
    });

    it("báo rõ lý do khi dataset đang được bài dùng", async () => {
        const table = await renderMyDatasets();

        fireEvent.click(rowOf(table, "Trường học").getByRole("button", { name: "Xoá Trường học" }));
        fireEvent.click(
            within(await screen.findByRole("alertdialog")).getByRole("button", {
                name: "Xoá dataset",
            }),
        );

        expect(
            await screen.findByText(
                "Chưa xoá được dataset. Dataset đang được 2 bài dùng nên chưa xoá được",
            ),
        ).toBeInTheDocument();
        expect(namesIn(table)).toContain("Trường học");
    });
});
