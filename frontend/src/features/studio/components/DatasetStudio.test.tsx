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

    it("hiện tên, mô tả và các bảng của schema, không hiện phiên bản", async () => {
        renderStudio();

        expect(await screen.findByRole("heading", { name: "Công ty" })).toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Tên database" })).toHaveValue("Công ty");
        expect(screen.getByRole("textbox", { name: "Mô tả" })).toHaveValue(
            "Phòng ban và nhân viên kèm lương",
        );
        expect(screen.queryByText(/Phiên bản/)).not.toBeInTheDocument();
        expect(screen.getByRole("textbox", { name: "Tên bảng 1" })).toHaveValue("departments");
        expect(screen.getByRole("textbox", { name: "Tên cột 2, bảng employees" })).toHaveValue(
            "full_name",
        );
        expect(screen.getByRole("combobox", { name: "Kiểu, cột 3, bảng employees" })).toHaveValue(
            "DECIMAL",
        );
        expect(screen.getByRole("link", { name: "Quay lại Database của tôi" })).toHaveAttribute(
            "href",
            "/studio/databases",
        );
    });

    it("bấm Lưu thì gửi cả schema sau khi đổi tên cột rồi báo đã lưu", async () => {
        const puts = recordSchemaPuts();
        renderStudio();

        fireEvent.change(
            await screen.findByRole("textbox", { name: "Tên cột 2, bảng departments" }),
            {
                target: { value: "title" },
            },
        );

        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
        expect(await screen.findByText(/^Đã lưu \d{2}:\d{2}$/)).toBeInTheDocument();
        expect(puts).toHaveLength(1);
        expect(puts[0].map((table) => table.name)).toEqual(["departments", "employees"]);
        expect(puts[0][0].columns[1]).toMatchObject({ name: "title", type: "TEXT" });
    });

    it("thêm bảng mới có sẵn cột khoá chính id", async () => {
        const puts = recordSchemaPuts();
        renderStudio();

        fireEvent.click(await screen.findByRole("button", { name: "Thêm bảng" }));

        expect(screen.getByRole("textbox", { name: "Tên bảng 3" })).toHaveValue("new_table");
        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
        await screen.findByText(/^Đã lưu/);
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

    it("chưa bấm Lưu thì chưa gửi gì, nút Lưu chỉ bật khi có thay đổi", async () => {
        const puts = recordSchemaPuts();
        renderStudio();

        const save = await screen.findByRole("button", { name: "Lưu" });
        expect(save).toBeDisabled();
        fireEvent.change(screen.getByRole("textbox", { name: "Tên bảng 1" }), {
            target: { value: "teams" },
        });

        expect(screen.getByText("Có thay đổi chưa lưu")).toBeInTheDocument();
        expect(save).toBeEnabled();
        await new Promise((resolve) => setTimeout(resolve, 1200));
        expect(puts).toEqual([]);
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

        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
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

        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
        await screen.findByText(/^Đã lưu/);
        expect(puts[0].map((table) => table.name)).toEqual(["employees"]);
        expect(puts[0][0].columns[3].references).toBeNull();
    });

    it("database đã chốt vẫn sửa được như bình thường", async () => {
        const puts = recordSchemaPuts();
        renderStudio(SCHOOL_ID);

        const tableName = await screen.findByRole("textbox", { name: "Tên bảng 1" });
        expect(tableName).toBeEnabled();
        expect(screen.queryByRole("button", { name: "Tạo bản nháp mới" })).not.toBeInTheDocument();
        fireEvent.change(tableName, { target: { value: "school_classes" } });

        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
        expect(await screen.findByText(/^Đã lưu \d{2}:\d{2}$/)).toBeInTheDocument();
        expect(puts[0][0].name).toBe("school_classes");
    });

    it("bấm Lưu thì lưu tên database và cập nhật tiêu đề", async () => {
        const patches: unknown[] = [];
        server.events.on("request:start", async ({ request }) => {
            if (request.method === "PATCH") {
                patches.push(await request.clone().json());
            }
        });
        renderStudio();

        fireEvent.change(await screen.findByRole("textbox", { name: "Tên database" }), {
            target: { value: "Tập đoàn" },
        });

        fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
        expect(await screen.findByRole("heading", { name: "Tập đoàn" })).toBeInTheDocument();
        expect(patches).toEqual([{ name: "Tập đoàn" }]);
    });

    it("tên database trống thì báo lỗi và không lưu", async () => {
        renderStudio();

        fireEvent.change(await screen.findByRole("textbox", { name: "Tên database" }), {
            target: { value: "  " },
        });

        expect(await screen.findByText("Không được để trống")).toBeInTheDocument();
        expect(screen.getByRole("heading", { name: "Công ty" })).toBeInTheDocument();
    });

    it("báo không tìm thấy khi database không tồn tại hoặc không phải của mình", async () => {
        renderStudio("0192f3e0-0000-7000-8000-ffffffffffff");

        expect(await screen.findByText("Không tìm thấy database này.")).toBeInTheDocument();
    });
});
