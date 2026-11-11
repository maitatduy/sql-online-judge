import { fireEvent, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { Workspace } from "./Workspace";

vi.mock("next/navigation", () => ({ notFound: vi.fn() }));

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

async function openWorkspace() {
    renderWithProviders(<Workspace slug={SLUG} />);
    return screen.findByRole("combobox", { name: "Dialect" });
}

describe("chọn dialect trong Workspace", () => {
    beforeEach(() => {
        localStorage.clear();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("chỉ liệt kê các dialect bài hỗ trợ, mặc định dialect đầu tiên", async () => {
        const dialect = await openWorkspace();

        expect(dialect).toHaveValue("POSTGRESQL");
        expect(
            Array.from((dialect as HTMLSelectElement).options).map((option) => option.text),
        ).toEqual(["PostgreSQL 16", "MySQL 8.4", "SQLite 3.46"]);
    });

    it("chạy thử và nộp bài bằng dialect đã chọn", async () => {
        const sentDialects: string[] = [];
        server.use(
            http.post("*/api/problems/:problemId/runs", async ({ request }) => {
                sentDialects.push(((await request.json()) as { dialect: string }).dialect);
                return HttpResponse.json({ runId: "run-1" }, { status: 202 });
            }),
            http.post("*/api/problems/:problemId/submissions", async ({ request }) => {
                sentDialects.push(((await request.json()) as { dialect: string }).dialect);
                return HttpResponse.json({ submissionId: "sub-1" }, { status: 202 });
            }),
        );
        const dialect = await openWorkspace();

        fireEvent.change(dialect, { target: { value: "MYSQL" } });
        fireEvent.click(screen.getByRole("button", { name: "Run" }));
        fireEvent.click(screen.getByRole("button", { name: "Submit" }));

        await waitFor(() => expect(sentDialects).toEqual(["MYSQL", "MYSQL"]));
    });

    it("mỗi dialect giữ nháp riêng", async () => {
        const dialect = await openWorkspace();
        const editor = screen.getByRole("textbox", { name: "Mã SQL của bạn" });

        fireEvent.change(editor, { target: { value: "SELECT 'postgres';" } });
        fireEvent.change(dialect, { target: { value: "SQLITE" } });
        expect(editor).not.toHaveValue("SELECT 'postgres';");
        fireEvent.change(editor, { target: { value: "SELECT 'sqlite';" } });
        fireEvent.change(dialect, { target: { value: "POSTGRESQL" } });
        expect(editor).toHaveValue("SELECT 'postgres';");
        fireEvent.change(dialect, { target: { value: "SQLITE" } });
        expect(editor).toHaveValue("SELECT 'sqlite';");
    });

    it("mở lại bài thì giữ dialect đã chọn lần trước", async () => {
        localStorage.setItem("dialect:0192f3b0-0000-7000-8000-0000000000b8", "SQLITE");

        expect(await openWorkspace()).toHaveValue("SQLITE");
    });
});
