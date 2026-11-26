import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import type { LessonCell } from "../types";
import { LessonSqlCell } from "./LessonSqlCell";

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

const CELL: LessonCell = {
    cellKey: "cell-1",
    starterSql: "SELECT id, name FROM departments;",
    dialect: "POSTGRESQL",
};

function editor() {
    return screen.getByRole("textbox", { name: "Câu SQL" });
}

describe("LessonSqlCell", () => {
    beforeEach(() => {
        localStorage.clear();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("chạy câu mẫu và hiện bảng kết quả kèm số dòng và thời gian", async () => {
        renderWithProviders(<LessonSqlCell cell={CELL} />);

        expect(editor()).toHaveValue("SELECT id, name FROM departments;");
        fireEvent.click(screen.getByRole("button", { name: "Chạy" }));

        const table = await screen.findByRole("table", { name: "Kết quả" });
        expect(within(table).getByText("Kỹ thuật")).toBeInTheDocument();
        expect(screen.getByText("2 dòng, 12 ms")).toBeInTheDocument();
    });

    it("đổi sang MySQL thì chạy trên MySQL", async () => {
        let requestedDialect: unknown;
        server.use(
            http.post("*/api/lesson-cells/:cellKey/runs", async ({ request }) => {
                requestedDialect = ((await request.json()) as { dialect?: string }).dialect;
                return HttpResponse.json({ runId: "run-mysql" }, { status: 202 });
            }),
        );
        renderWithProviders(<LessonSqlCell cell={CELL} />);

        const dialects = within(screen.getByRole("radiogroup", { name: "Hệ quản trị" }));
        expect(dialects.getByRole("radio", { name: "PostgreSQL" })).toBeChecked();
        fireEvent.click(dialects.getByRole("radio", { name: "MySQL" }));
        fireEvent.click(screen.getByRole("button", { name: "Chạy" }));

        await vi.waitFor(() => expect(requestedDialect).toBe("MYSQL"));
        expect(dialects.getByRole("radio", { name: "SQLite" })).toBeInTheDocument();
    });

    it("lệnh bị cấm hiện mã FS và thông báo, không có bảng", async () => {
        renderWithProviders(<LessonSqlCell cell={CELL} />);

        fireEvent.change(editor(), { target: { value: "DROP TABLE employees" } });
        fireEvent.click(screen.getByRole("button", { name: "Chạy" }));

        expect(await screen.findByText("Bài này chỉ cho phép SELECT, WITH")).toBeInTheDocument();
        expect(screen.getByText("FS")).toBeInTheDocument();
        expect(screen.queryByRole("table")).not.toBeInTheDocument();
    });

    it("giữ bản nháp khi mở lại và đặt lại về câu mẫu", () => {
        const { unmount } = renderWithProviders(<LessonSqlCell cell={CELL} />);
        fireEvent.change(editor(), { target: { value: "SELECT 1;" } });
        unmount();

        renderWithProviders(<LessonSqlCell cell={CELL} />);
        expect(editor()).toHaveValue("SELECT 1;");

        fireEvent.click(screen.getByRole("button", { name: "Đặt lại" }));
        expect(editor()).toHaveValue("SELECT id, name FROM departments;");
        expect(screen.queryByRole("button", { name: "Đặt lại" })).not.toBeInTheDocument();
    });
});
