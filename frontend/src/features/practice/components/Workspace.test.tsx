import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { renderWithProviders } from "@/test/render";

import { Workspace } from "./Workspace";

const notFound = vi.hoisted(() => vi.fn());

vi.mock("next/navigation", () => ({ notFound }));

vi.mock("@/components/shared/LazySqlEditor", () => ({
    LazySqlEditor: ({
        value,
        onChange,
        ariaLabel,
    }: {
        value: string;
        onChange: (value: string) => void;
        ariaLabel: string;
    }) => (
        <textarea
            aria-label={ariaLabel}
            value={value}
            onChange={(event) => onChange(event.target.value)}
        />
    ),
}));

const SLUG = "highest-salary-per-department";
const EDITOR_NAME = "Mã SQL của bạn";

async function renderWorkspace(slug = SLUG) {
    renderWithProviders(<Workspace slug={slug} />);
    return screen.findByRole("heading", { level: 1 });
}

describe("Workspace", () => {
    beforeEach(() => {
        localStorage.clear();
        notFound.mockReset();
    });

    it("hiện đề bài: tiêu đề, độ khó, chủ đề, thống kê và nội dung Markdown", async () => {
        const heading = await renderWorkspace();

        expect(heading).toHaveTextContent("Lương cao nhất mỗi phòng ban");
        const statement = within(screen.getByRole("tabpanel", { name: "Đề bài" }));
        expect(statement.getByText("Trung bình")).toHaveClass("text-difficulty-medium");
        expect(statement.getByText("Subquery")).toBeInTheDocument();
        expect(statement.getByText("AC 41,2% · 12.380 người giải")).toBeInTheDocument();
        expect(
            statement.getByText("lương cao nhất trong mỗi phòng ban", { selector: "strong" }),
        ).toBeInTheDocument();
    });

    it("hiện ví dụ kết quả mong đợi và các ràng buộc của bài", async () => {
        await renderWorkspace();
        const statement = within(screen.getByRole("tabpanel", { name: "Đề bài" }));

        const example = within(statement.getByRole("table", { name: "Ví dụ kết quả mong đợi" }));
        expect(example.getAllByRole("row")).toHaveLength(5);
        expect(statement.getByText(/Bắt buộc đúng thứ tự dòng/)).toBeInTheDocument();
        expect(statement.getByText("Chỉ cho phép SELECT, WITH.")).toBeInTheDocument();
        expect(
            statement.getByText("Giới hạn thời gian 2 giây · 2 testcase ẩn."),
        ).toBeInTheDocument();
    });

    it("cho xem schema: bảng, cột, kiểu, khoá chính, khoá ngoại", async () => {
        await renderWorkspace();

        fireEvent.mouseDown(screen.getByRole("tab", { name: "Schema" }));

        const schema = within(await screen.findByRole("tabpanel", { name: "Schema" }));
        const employees = within(schema.getByRole("table", { name: "employees" }));
        expect(employees.getByRole("row", { name: /id INTEGER Khoá chính/ })).toBeInTheDocument();
        expect(
            employees.getByRole("row", {
                name: /department_id INTEGER Khoá ngoại → departments\.id · Có thể NULL/,
            }),
        ).toBeInTheDocument();
    });

    it("mở mã khởi đầu trong editor và ghi nhãn dialect", async () => {
        await renderWorkspace();

        expect(screen.getByRole("textbox", { name: EDITOR_NAME })).toHaveValue(
            "SELECT *\nFROM employees;",
        );
        expect(screen.getByText("PostgreSQL 16")).toBeInTheDocument();
    });

    it("lưu nháp khi gõ và mở lại đúng nháp khi quay lại bài", async () => {
        await renderWorkspace();
        fireEvent.change(screen.getByRole("textbox", { name: EDITOR_NAME }), {
            target: { value: "SELECT name FROM employees;" },
        });

        expect(await screen.findByText(/^Đã lưu nháp · \d{2}:\d{2}$/)).toBeInTheDocument();
        expect(localStorage.getItem("draft:0192f3b0-0000-7000-8000-0000000000b8:POSTGRESQL")).toBe(
            "SELECT name FROM employees;",
        );
    });

    it("đặt lại về mã khởi đầu sau khi xác nhận", async () => {
        localStorage.setItem("draft:0192f3b0-0000-7000-8000-0000000000b8:POSTGRESQL", "SELECT 1;");
        await renderWorkspace();
        expect(screen.getByRole("textbox", { name: EDITOR_NAME })).toHaveValue("SELECT 1;");

        fireEvent.click(screen.getByRole("button", { name: "Đặt lại mã" }));
        const dialog = within(await screen.findByRole("alertdialog"));
        fireEvent.click(dialog.getByRole("button", { name: "Đặt lại" }));

        await waitFor(() =>
            expect(screen.getByRole("textbox", { name: EDITOR_NAME })).toHaveValue(
                "SELECT *\nFROM employees;",
            ),
        );
    });

    it("chuyển sang trang không tìm thấy khi bài không tồn tại", async () => {
        renderWithProviders(<Workspace slug="khong-co-bai-nay" />);

        await waitFor(() => expect(notFound).toHaveBeenCalled());
    });

    it("hiện trạng thái đang tải rồi lỗi có thể thử lại", async () => {
        let remainingFailures = 2;
        server.use(
            http.get(`*/api/problems/${SLUG}`, () => {
                if (remainingFailures === 0) {
                    return undefined;
                }
                remainingFailures -= 1;
                return HttpResponse.json(
                    { status: 500, code: "INTERNAL_ERROR", message: "Đã có lỗi xảy ra" },
                    { status: 500 },
                );
            }),
        );
        renderWithProviders(<Workspace slug={SLUG} />);

        expect(screen.getByRole("status", { name: "Đang tải đề bài" })).toBeInTheDocument();
        const alert = await screen.findByRole("alert");
        expect(alert).toHaveTextContent("Chưa tải được đề bài");

        fireEvent.click(within(alert).getByRole("button", { name: "Thử lại" }));

        expect(await screen.findByRole("heading", { level: 1 })).toHaveTextContent(
            "Lương cao nhất mỗi phòng ban",
        );
    });
});
