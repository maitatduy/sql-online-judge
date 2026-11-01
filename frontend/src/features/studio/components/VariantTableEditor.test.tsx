import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { DatasetStudio } from "./DatasetStudio";

const COMPANY_ID = "0192f3e0-0000-7000-8000-000000000001";

async function openTable(variantName: string, tableName: string) {
    renderWithProviders(
        <ToastProvider>
            <DatasetStudio datasetId={COMPANY_ID} />
        </ToastProvider>,
    );
    fireEvent.mouseDown(await screen.findByRole("tab", { name: "Bộ dữ liệu" }));
    const card = within(await screen.findByRole("listitem", { name: variantName }));
    fireEvent.click(card.getByRole("button", { name: `Nhập dữ liệu bảng ${tableName}` }));
    return within(
        await screen.findByRole("region", { name: `Bảng ${tableName}, bộ ${variantName}` }),
    );
}

function recordCsvPuts(): string[] {
    const bodies: string[] = [];
    server.events.on("request:start", async ({ request }) => {
        if (request.method === "PUT" && request.url.includes("/tables/")) {
            bodies.push(((await request.clone().json()) as { csv: string }).csv);
        }
    });
    return bodies;
}

function cell(editor: ReturnType<typeof within>, column: string, row: number) {
    return editor.getByRole("textbox", { name: `${column}, dòng ${row}` });
}

describe("DatasetStudio - nhập dữ liệu cho bảng", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
        server.events.removeAllListeners();
    });

    it("mở bảng đã có dữ liệu, ô trống hiển thị là NULL", async () => {
        const editor = await openTable("Mẫu", "employees");

        expect(await editor.findByRole("textbox", { name: "full_name, dòng 1" })).toHaveValue(
            "Hoàng Quốc Bảo",
        );
        expect(editor.getAllByRole("row")).toHaveLength(7);
        expect(cell(editor, "department_id", 6)).toHaveValue("");
        expect(cell(editor, "department_id", 6)).toHaveAttribute("placeholder", "NULL");
    });

    it("bảng chưa có dữ liệu thì bắt đầu trống", async () => {
        const editor = await openTable("Có NULL", "employees");

        expect(await editor.findByText("Chưa có dòng nào.")).toBeInTheDocument();
    });

    it("sửa ô và thêm dòng rồi bấm Lưu thì gửi CSV và cập nhật số dòng", async () => {
        const puts = recordCsvPuts();
        const editor = await openTable("Mẫu", "departments");

        fireEvent.change(await editor.findByRole("textbox", { name: "name, dòng 3" }), {
            target: { value: "Nhân sự, hành chính" },
        });
        fireEvent.click(editor.getByRole("button", { name: "Thêm dòng" }));
        fireEvent.change(cell(editor, "id", 4), { target: { value: "4" } });
        fireEvent.change(cell(editor, "name", 4), { target: { value: "Pháp chế" } });
        expect(editor.getByText("Có thay đổi chưa lưu")).toBeInTheDocument();
        fireEvent.click(editor.getByRole("button", { name: "Lưu" }));

        expect(await editor.findByText(/^Đã lưu \d{2}:\d{2}$/)).toBeInTheDocument();
        expect(puts).toEqual([
            'id,name\n1,Kỹ thuật\n2,Kinh doanh\n3,"Nhân sự, hành chính"\n4,Pháp chế\n',
        ]);
        expect(
            within(screen.getByRole("listitem", { name: "Mẫu" })).getByText("departments: 4 dòng"),
        ).toBeInTheDocument();
    });

    it("xoá dòng khỏi bảng", async () => {
        const puts = recordCsvPuts();
        const editor = await openTable("Mẫu", "departments");

        fireEvent.click(await editor.findByRole("button", { name: "Xoá dòng 1" }));
        fireEvent.click(editor.getByRole("button", { name: "Lưu" }));

        await editor.findByText(/^Đã lưu/);
        expect(puts).toEqual(["id,name\n2,Kinh doanh\n3,Nhân sự\n"]);
    });

    it("dán CSV thì thay toàn bộ dòng trong bảng, sai tiêu đề thì báo lỗi", async () => {
        const editor = await openTable("Mẫu", "departments");

        fireEvent.click(await editor.findByRole("button", { name: "Dán CSV" }));
        let dialog = within(await screen.findByRole("dialog", { name: "Dán CSV" }));
        fireEvent.change(dialog.getByRole("textbox", { name: "Nội dung CSV" }), {
            target: { value: "id,title\n1,A\n" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Dùng dữ liệu này" }));
        expect(
            await dialog.findByText("Dòng tiêu đề phải gồm các cột: id, name"),
        ).toBeInTheDocument();

        fireEvent.change(dialog.getByRole("textbox", { name: "Nội dung CSV" }), {
            target: { value: "id,name\n10,Kho vận\n" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Dùng dữ liệu này" }));

        expect(screen.queryByRole("dialog", { name: "Dán CSV" })).not.toBeInTheDocument();
        expect(cell(editor, "name", 1)).toHaveValue("Kho vận");
        expect(editor.getAllByRole("row")).toHaveLength(2);
        dialog = within(document.body);
        expect(dialog.getByText("Có thay đổi chưa lưu")).toBeInTheDocument();
    });

    it("tải file .csv thì điền dữ liệu vào bảng", async () => {
        const editor = await openTable("Mẫu", "departments");
        const file = new File(["id,name\n7,Đào tạo\n8,Mua hàng\n"], "phong-ban.csv", {
            type: "text/csv",
        });

        fireEvent.change(await editor.findByLabelText("Tải file .csv"), {
            target: { files: [file] },
        });

        expect(await editor.findByDisplayValue("Mua hàng")).toBeInTheDocument();
        expect(editor.getAllByRole("row")).toHaveLength(3);
    });

    it("hiện lỗi máy chủ khi dữ liệu sai, ví dụ trùng khoá chính", async () => {
        server.use(
            http.put("*/api/studio/datasets/:datasetId/variants/:variantId/tables/:table", () =>
                HttpResponse.json(
                    {
                        status: 400,
                        code: "VALIDATION_FAILED",
                        message: "Dữ liệu gửi lên không hợp lệ",
                        errors: [{ field: "csv", message: "Dòng 3 trùng khoá chính với dòng 2" }],
                    },
                    { status: 400 },
                ),
            ),
        );
        const editor = await openTable("Mẫu", "departments");

        fireEvent.change(await editor.findByRole("textbox", { name: "id, dòng 2" }), {
            target: { value: "1" },
        });
        fireEvent.click(editor.getByRole("button", { name: "Lưu" }));

        expect(await editor.findByText("Dòng 3 trùng khoá chính với dòng 2")).toBeInTheDocument();
    });
});
