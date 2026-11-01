import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { DatasetStudio } from "./DatasetStudio";

const COMPANY_ID = "0192f3e0-0000-7000-8000-000000000001";
const SCHOOL_ID = "0192f3e0-0000-7000-8000-000000000002";

async function openVariantsTab(datasetId = COMPANY_ID) {
    renderWithProviders(
        <ToastProvider>
            <DatasetStudio datasetId={datasetId} />
        </ToastProvider>,
    );
    fireEvent.mouseDown(await screen.findByRole("tab", { name: "Bộ dữ liệu" }));
    return screen.findByRole("region", { name: "Bộ dữ liệu" });
}

function variantCard(name: string) {
    return within(screen.getByRole("listitem", { name }));
}

describe("DatasetStudio - bộ dữ liệu", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("liệt kê bộ dữ liệu kèm công khai hay ẩn và số dòng của từng bảng", async () => {
        await openVariantsTab();

        const sample = variantCard("Mẫu");
        expect(sample.getByText("Công khai")).toBeInTheDocument();
        expect(sample.getByText("departments: 3 dòng")).toBeInTheDocument();
        expect(sample.getByText("employees: 6 dòng")).toBeInTheDocument();
        expect(variantCard("Có NULL").getByText("Ẩn")).toBeInTheDocument();
        expect(variantCard("Có NULL").getByText("employees: chưa có dữ liệu")).toBeInTheDocument();
    });

    it("báo khi database chưa có bộ dữ liệu nào", async () => {
        await openVariantsTab(SCHOOL_ID);

        expect(screen.getByText("Chưa có bộ dữ liệu nào.")).toBeInTheDocument();
    });

    it("thêm bộ dữ liệu ẩn mới", async () => {
        await openVariantsTab();

        fireEvent.click(screen.getByRole("button", { name: "Thêm bộ dữ liệu" }));
        const dialog = within(await screen.findByRole("dialog", { name: "Thêm bộ dữ liệu" }));
        fireEvent.change(dialog.getByRole("textbox", { name: "Tên bộ dữ liệu" }), {
            target: { value: "Lương bằng nhau" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Thêm" }));

        expect(
            await screen.findByRole("listitem", { name: "Lương bằng nhau" }),
        ).toBeInTheDocument();
        expect(variantCard("Lương bằng nhau").getByText("Ẩn")).toBeInTheDocument();
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });

    it("báo lỗi trong hộp thoại khi tên bộ dữ liệu đã có", async () => {
        await openVariantsTab();

        fireEvent.click(screen.getByRole("button", { name: "Thêm bộ dữ liệu" }));
        const dialog = within(await screen.findByRole("dialog", { name: "Thêm bộ dữ liệu" }));
        fireEvent.change(dialog.getByRole("textbox", { name: "Tên bộ dữ liệu" }), {
            target: { value: "Mẫu" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Thêm" }));

        expect(await dialog.findByText("Tên bộ dữ liệu đã được dùng")).toBeInTheDocument();
    });

    it("đổi tên và chuyển bộ dữ liệu sang công khai", async () => {
        await openVariantsTab();

        fireEvent.click(variantCard("Có NULL").getByRole("button", { name: "Sửa Có NULL" }));
        const dialog = within(await screen.findByRole("dialog", { name: "Sửa bộ dữ liệu" }));
        fireEvent.change(dialog.getByRole("textbox", { name: "Tên bộ dữ liệu" }), {
            target: { value: "Thiếu phòng ban" },
        });
        fireEvent.click(dialog.getByRole("checkbox", { name: "Công khai cho học viên" }));
        fireEvent.click(dialog.getByRole("button", { name: "Lưu" }));

        expect(
            await screen.findByRole("listitem", { name: "Thiếu phòng ban" }),
        ).toBeInTheDocument();
        expect(variantCard("Thiếu phòng ban").getByText("Công khai")).toBeInTheDocument();
    });

    it("xoá bộ dữ liệu sau khi xác nhận", async () => {
        await openVariantsTab();

        fireEvent.click(variantCard("Có NULL").getByRole("button", { name: "Xoá Có NULL" }));
        const dialog = within(await screen.findByRole("alertdialog"));
        expect(dialog.getByText("Xoá bộ dữ liệu Có NULL?")).toBeInTheDocument();
        fireEvent.click(dialog.getByRole("button", { name: "Xoá bộ dữ liệu" }));

        expect(await screen.findByText("Đã xoá bộ dữ liệu Có NULL.")).toBeInTheDocument();
        expect(screen.queryByRole("listitem", { name: "Có NULL" })).not.toBeInTheDocument();
    });
});
