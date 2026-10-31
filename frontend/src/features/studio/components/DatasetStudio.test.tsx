import { fireEvent, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import type { DatasetTable } from "@/features/studio/types";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { DatasetStudio } from "./DatasetStudio";

const COMPANY_ID = "0192f3e0-0000-7000-8000-000000000001";
const SCHOOL_ID = "0192f3e0-0000-7000-8000-000000000002";

function renderStudio(datasetId = COMPANY_ID) {
    renderWithProviders(
        <ToastProvider>
            <DatasetStudio datasetId={datasetId} />
        </ToastProvider>,
    );
}

function recordSchemaPuts(): DatasetTable[][] {
    const bodies: DatasetTable[][] = [];
    server.events.on("request:start", async ({ request }) => {
        if (request.method === "PUT") {
            bodies.push(((await request.clone().json()) as { tables: DatasetTable[] }).tables);
        }
    });
    return bodies;
}

describe("DatasetStudio - soạn schema", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
        server.events.removeAllListeners();
    });

    it("hiện tên, phiên bản nháp và các bảng của schema", async () => {
        renderStudio();

        expect(await screen.findByRole("heading", { name: "Công ty" })).toBeInTheDocument();
        expect(screen.getByText("Phiên bản 2 · Nháp")).toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Tên bảng 1" })).toHaveValue("departments");
        expect(screen.getByRole("textbox", { name: "Tên cột 2, bảng employees" })).toHaveValue(
            "full_name",
        );
        expect(screen.getByRole("combobox", { name: "Kiểu, cột 3, bảng employees" })).toHaveValue(
            "DECIMAL",
        );
        expect(screen.getByRole("link", { name: "Quay lại Dataset của tôi" })).toHaveAttribute(
            "href",
            "/studio/datasets",
        );
    });

    it("tự lưu cả schema sau khi đổi tên cột rồi báo đã lưu", async () => {
        const puts = recordSchemaPuts();
        renderStudio();

        fireEvent.change(
            await screen.findByRole("textbox", { name: "Tên cột 2, bảng departments" }),
            {
                target: { value: "title" },
            },
        );

        expect(await screen.findByText(/^Bản nháp · lưu \d{2}:\d{2}$/)).toBeInTheDocument();
        expect(puts).toHaveLength(1);
        expect(puts[0].map((table) => table.name)).toEqual(["departments", "employees"]);
        expect(puts[0][0].columns[1]).toMatchObject({ name: "title", type: "TEXT" });
    });

    it("thêm bảng mới có sẵn cột khoá chính id", async () => {
        const puts = recordSchemaPuts();
        renderStudio();

        fireEvent.click(await screen.findByRole("button", { name: "Thêm bảng" }));

        expect(screen.getByRole("textbox", { name: "Tên bảng 3" })).toHaveValue("new_table");
        await screen.findByText(/^Bản nháp · lưu/);
        expect(puts[0][2]).toEqual({
            name: "new_table",
            columns: [
                {
                    name: "id",
                    type: "INTEGER",
                    isPrimaryKey: true,
                    isNullable: false,
                    references: null,
                },
            ],
        });
    });

    it("tên cột có dấu thì báo lỗi ngay và không gửi lên máy chủ", async () => {
        const puts = recordSchemaPuts();
        renderStudio();

        const name = await screen.findByRole("textbox", { name: "Tên cột 2, bảng departments" });
        fireEvent.change(name, { target: { value: "Tên" } });

        expect(
            await screen.findByText(
                "Chỉ gồm chữ thường không dấu, số và dấu gạch dưới, không bắt đầu bằng số",
            ),
        ).toBeInTheDocument();
        expect(name).toHaveAttribute("aria-invalid", "true");
        await new Promise((resolve) => setTimeout(resolve, 1200));
        expect(puts).toEqual([]);
    });

    it("hiện lỗi máy chủ dưới đúng ô theo đường dẫn trường", async () => {
        renderStudio();

        fireEvent.change(await screen.findByRole("textbox", { name: "Tên bảng 1" }), {
            target: { value: "order" },
        });

        expect(await screen.findByText("Tên này trùng từ khoá SQL")).toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Tên bảng 1" })).toHaveAttribute(
            "aria-invalid",
            "true",
        );
    });

    it("ô khoá ngoại chỉ liệt kê cột khoá chính cùng kiểu", async () => {
        renderStudio();

        const reference = await screen.findByRole("combobox", {
            name: "Khoá ngoại, cột 4, bảng employees",
        });

        expect(reference).toHaveValue("departments.id");
        expect(
            within(reference)
                .getAllByRole("option")
                .map((option) => option.textContent),
        ).toEqual(["Không", "departments.id", "employees.id"]);
    });

    it("xoá bảng thì bỏ luôn khoá ngoại đang trỏ tới bảng đó", async () => {
        const puts = recordSchemaPuts();
        renderStudio();

        fireEvent.click(await screen.findByRole("button", { name: "Xoá bảng departments" }));

        await screen.findByText(/^Bản nháp · lưu/);
        expect(puts[0].map((table) => table.name)).toEqual(["employees"]);
        expect(puts[0][0].columns[3].references).toBeNull();
    });

    it("phiên bản đã chốt thì khoá các ô, tạo bản nháp mới để sửa tiếp", async () => {
        renderStudio(SCHOOL_ID);

        expect(
            await screen.findByText("Phiên bản 1 đã chốt nên không sửa được."),
        ).toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Tên bảng 1" })).toBeDisabled();

        fireEvent.click(screen.getByRole("button", { name: "Tạo bản nháp mới" }));

        expect(await screen.findByText("Phiên bản 2 · Nháp")).toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Tên bảng 1" })).toBeEnabled();
    });

    it("báo không tìm thấy khi dataset không tồn tại hoặc không phải của mình", async () => {
        renderStudio("0192f3e0-0000-7000-8000-ffffffffffff");

        expect(await screen.findByText("Không tìm thấy dataset này.")).toBeInTheDocument();
    });
});
