import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { LessonEditor } from "./LessonEditor";

const DRAFT_COURSE_ID = "0192f3f1-0000-7000-8000-000000000001";
const DRAFT_LESSON_ID = "0192f3f3-0000-7000-8000-000000000001";
const IN_REVIEW_COURSE_ID = "0192f3f1-0000-7000-8000-000000000002";
const IN_REVIEW_LESSON_ID = "0192f3f3-0000-7000-8000-000000000005";
const SHOP_DATASET_ID = "0192f3e0-0000-7000-8000-000000000003";
const COMPANY_DATASET_ID = "0192f3e0-0000-7000-8000-000000000001";

async function renderCells(courseId = DRAFT_COURSE_ID, lessonId = DRAFT_LESSON_ID) {
    renderWithProviders(
        <ToastProvider>
            <LessonEditor courseId={courseId} lessonId={lessonId} />
        </ToastProvider>,
    );
    return within(await screen.findByRole("region", { name: "Ô SQL chạy thử" }));
}

async function selectReady(cells: ReturnType<typeof within>) {
    const select = await cells.findByRole("combobox", { name: "Database" });
    await within(select).findByRole("option", { name: "Cửa hàng trực tuyến" });
}

describe("ô SQL trong trình soạn bài giảng", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("liệt kê các ô SQL đã gắn kèm dialect và câu SQL mở đầu", async () => {
        const cells = await renderCells();

        const cell = within(await cells.findByRole("group", { name: "Ô SQL 1" }));
        expect(cell.getByText("PostgreSQL")).toBeInTheDocument();
        expect(cell.getByText("SELECT * FROM customers;")).toBeInTheDocument();
    });

    it("thêm ô SQL trên bộ dữ liệu công khai của database đã chốt", async () => {
        const cells = await renderCells();

        fireEvent.click(await cells.findByRole("button", { name: "Thêm ô SQL" }));
        await selectReady(cells);
        fireEvent.change(await cells.findByRole("combobox", { name: "Database" }), {
            target: { value: SHOP_DATASET_ID },
        });
        const variant = await cells.findByRole("combobox", { name: "Bộ dữ liệu" });
        await within(variant).findByRole("option", { name: "Mẫu" });
        fireEvent.change(variant, { target: { value: "0192f3e1-0000-7000-8000-000000000003" } });
        fireEvent.change(cells.getByRole("combobox", { name: "Dialect" }), {
            target: { value: "MYSQL" },
        });
        fireEvent.change(cells.getByRole("textbox", { name: "Câu SQL mở đầu" }), {
            target: { value: "SELECT full_name FROM customers;" },
        });
        fireEvent.click(cells.getByRole("button", { name: "Lưu ô SQL" }));

        const added = within(await cells.findByRole("group", { name: "Ô SQL 2" }));
        expect(added.getByText("MySQL")).toBeInTheDocument();
        expect(added.getByText("SELECT full_name FROM customers;")).toBeInTheDocument();
    });

    it("database còn bản nháp chưa chốt thì không chọn được bộ dữ liệu", async () => {
        const cells = await renderCells();

        fireEvent.click(await cells.findByRole("button", { name: "Thêm ô SQL" }));
        await selectReady(cells);
        fireEvent.change(await cells.findByRole("combobox", { name: "Database" }), {
            target: { value: COMPANY_DATASET_ID },
        });

        expect(
            await cells.findByText("Database còn bản nháp chưa chốt. Hãy chốt trước khi gắn."),
        ).toBeInTheDocument();
        expect(cells.queryByRole("combobox", { name: "Bộ dữ liệu" })).not.toBeInTheDocument();
        expect(cells.getByRole("button", { name: "Lưu ô SQL" })).toBeDisabled();
    });

    it("sửa câu SQL của ô rồi xoá ô sau khi xác nhận", async () => {
        const cells = await renderCells();

        fireEvent.click(await cells.findByRole("button", { name: "Sửa ô SQL 1" }));
        fireEvent.change(cells.getByRole("textbox", { name: "Câu SQL mở đầu" }), {
            target: { value: "SELECT id FROM orders;" },
        });
        fireEvent.click(cells.getByRole("button", { name: "Lưu ô SQL" }));
        expect(await cells.findByText("SELECT id FROM orders;")).toBeInTheDocument();

        fireEvent.click(cells.getByRole("button", { name: "Xoá ô SQL 1" }));
        fireEvent.click(
            within(await screen.findByRole("alertdialog")).getByRole("button", {
                name: "Xoá ô SQL",
            }),
        );
        expect(await cells.findByText("Bài giảng chưa có ô SQL nào.")).toBeInTheDocument();
    });

    it("khoá đang chờ duyệt chỉ hiện ô SQL, không có nút sửa", async () => {
        const cells = await renderCells(IN_REVIEW_COURSE_ID, IN_REVIEW_LESSON_ID);

        await cells.findByText("Bài giảng chưa có ô SQL nào.");
        expect(cells.queryByRole("button", { name: "Thêm ô SQL" })).not.toBeInTheDocument();
    });
});
