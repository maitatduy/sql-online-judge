import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse, type JsonBodyType } from "msw";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { DatasetStudio } from "./DatasetStudio";

const SCHOOL_ID = "0192f3e0-0000-7000-8000-000000000002";
const COMPANY_ID = "0192f3e0-0000-7000-8000-000000000001";
const PROJECTS_SQL =
    "CREATE TABLE projects (id INT PRIMARY KEY, title TEXT NOT NULL);\n" +
    "INSERT INTO projects (id, title) VALUES (1, 'Kho dữ liệu'), (2, 'Báo cáo');";
const PROJECTS_PREVIEW = {
    tables: [
        {
            name: "projects",
            columns: [
                {
                    name: "id",
                    type: "INTEGER",
                    isPrimaryKey: true,
                    isNullable: false,
                    references: null,
                },
                {
                    name: "title",
                    type: "TEXT",
                    isPrimaryKey: false,
                    isNullable: false,
                    references: null,
                },
            ],
        },
    ],
    data: [{ tableName: "projects", csv: "id,title\n1,Kho dữ liệu\n2,Báo cáo\n", rowCount: 2 }],
};

function useParseResult(body: JsonBodyType, status = 200) {
    server.use(
        http.post("*/api/studio/datasets/parse-sql", () => HttpResponse.json(body, { status })),
    );
}

function recordWrites(): string[] {
    const writes: string[] = [];
    server.events.on("request:start", ({ request }) => {
        if (request.method !== "GET" && !request.url.includes("parse-sql")) {
            writes.push(
                `${request.method} ${new URL(request.url).pathname.split("/").slice(4).join("/")}`,
            );
        }
    });
    return writes;
}

async function openImport(datasetId = SCHOOL_ID) {
    renderWithProviders(
        <ToastProvider>
            <DatasetStudio datasetId={datasetId} />
        </ToastProvider>,
    );
    fireEvent.click(await screen.findByRole("button", { name: "Nhập từ file SQL" }));
    return within(await screen.findByRole("dialog", { name: "Nhập từ file SQL" }));
}

describe("DatasetStudio - nhập từ file SQL", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
        server.events.removeAllListeners();
    });

    it("đọc file rồi xem trước bảng, số cột, số dòng và vài dòng đầu", async () => {
        useParseResult(PROJECTS_PREVIEW);
        const dialog = await openImport();

        fireEvent.change(dialog.getByRole("textbox", { name: "Nội dung SQL" }), {
            target: { value: PROJECTS_SQL },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Đọc file" }));

        expect(await dialog.findByText("projects: 2 cột, 2 dòng")).toBeInTheDocument();
        expect(dialog.getByRole("cell", { name: "Kho dữ liệu" })).toBeInTheDocument();
    });

    it("áp dụng rồi bấm Lưu thì lưu schema, tạo bộ dữ liệu mới và lưu dữ liệu", async () => {
        useParseResult(PROJECTS_PREVIEW);
        const writes = recordWrites();
        const dialog = await openImport();
        fireEvent.change(dialog.getByRole("textbox", { name: "Nội dung SQL" }), {
            target: { value: PROJECTS_SQL },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Đọc file" }));
        await dialog.findByText("projects: 2 cột, 2 dòng");
        fireEvent.change(dialog.getByRole("textbox", { name: "Tên bộ dữ liệu mới" }), {
            target: { value: "Từ file SQL" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Áp dụng" }));

        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Tên bảng 1" })).toHaveValue("projects");
        expect(
            screen.getByText("Dữ liệu của 1 bảng sẽ được lưu vào bộ Từ file SQL khi bấm Lưu."),
        ).toBeInTheDocument();
        expect(writes).toEqual([]);
        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

        expect(await screen.findByText(/^Đã lưu \d{2}:\d{2}$/)).toBeInTheDocument();
        expect(writes).toEqual([
            `PUT ${SCHOOL_ID}/schema`,
            `POST ${SCHOOL_ID}/variants`,
            expect.stringMatching(new RegExp(`^PUT ${SCHOOL_ID}/variants/.+/tables/projects$`)),
        ]);
        fireEvent.mouseDown(screen.getByRole("tab", { name: "Bộ dữ liệu" }));
        expect(
            within(await screen.findByRole("listitem", { name: "Từ file SQL" })).getByText(
                "projects: 2 dòng",
            ),
        ).toBeInTheDocument();
    });

    it("file chỉ có INSERT thì giữ schema, đưa dữ liệu vào bộ có sẵn", async () => {
        useParseResult({
            tables: [],
            data: [{ tableName: "departments", csv: "id,name\n9,Kho vận\n", rowCount: 1 }],
        });
        const writes = recordWrites();
        const dialog = await openImport(COMPANY_ID);
        fireEvent.change(dialog.getByRole("textbox", { name: "Nội dung SQL" }), {
            target: { value: "INSERT INTO departments (id, name) VALUES (9, 'Kho vận');" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Đọc file" }));
        await dialog.findByText("departments: 1 dòng");
        fireEvent.change(dialog.getByRole("combobox", { name: "Đưa dữ liệu vào bộ" }), {
            target: { value: "0192f3e1-0000-7000-8000-000000000001" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Áp dụng" }));

        expect(screen.getByRole("textbox", { name: "Tên bảng 1" })).toHaveValue("departments");
        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));

        await screen.findByText(/^Đã lưu/);
        expect(writes).toEqual([
            `PUT ${COMPANY_ID}/variants/0192f3e1-0000-7000-8000-000000000001/tables/departments`,
        ]);
    });

    it("báo lỗi đọc file ngay trong hộp thoại", async () => {
        useParseResult(
            {
                status: 400,
                code: "VALIDATION_FAILED",
                message: "Dữ liệu gửi lên không hợp lệ",
                errors: [
                    { field: "sql", message: "Câu lệnh thứ 2: chỉ nhận CREATE TABLE và INSERT" },
                ],
            },
            400,
        );
        const dialog = await openImport();

        fireEvent.change(dialog.getByRole("textbox", { name: "Nội dung SQL" }), {
            target: { value: "INSERT INTO a (id) VALUES (1); DROP TABLE a;" },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Đọc file" }));

        expect(
            await dialog.findByText("Câu lệnh thứ 2: chỉ nhận CREATE TABLE và INSERT"),
        ).toBeInTheDocument();
        expect(dialog.queryByRole("button", { name: "Áp dụng" })).not.toBeInTheDocument();
    });

    it("chọn file .sql thì điền nội dung vào ô", async () => {
        const dialog = await openImport();
        const file = new File([PROJECTS_SQL], "projects.sql", { type: "application/sql" });

        fireEvent.change(dialog.getByLabelText("Chọn file .sql"), { target: { files: [file] } });

        await waitFor(() =>
            expect(dialog.getByRole("textbox", { name: "Nội dung SQL" })).toHaveValue(PROJECTS_SQL),
        );
    });

    it("huỷ nhập thì trả schema về như trước và không lưu gì", async () => {
        useParseResult(PROJECTS_PREVIEW);
        const dialog = await openImport();
        fireEvent.change(dialog.getByRole("textbox", { name: "Nội dung SQL" }), {
            target: { value: PROJECTS_SQL },
        });
        fireEvent.click(dialog.getByRole("button", { name: "Đọc file" }));
        await dialog.findByText("projects: 2 cột, 2 dòng");
        fireEvent.click(dialog.getByRole("button", { name: "Áp dụng" }));

        fireEvent.click(screen.getByRole("button", { name: "Huỷ nhập" }));

        expect(screen.getByRole("textbox", { name: "Tên bảng 1" })).toHaveValue("classes");
        expect(screen.getByRole("button", { name: "Lưu" })).toBeDisabled();
    });
});
