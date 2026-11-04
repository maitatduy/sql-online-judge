import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { ToastProvider } from "@/components/shared/ToastProvider";
import { server } from "@/mocks/server";
import { SETTER_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import { ProblemStudio } from "./ProblemStudio";

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

const WITH_DATASET_ID = "0192f3d0-0000-7000-8000-000000000003";

async function saveAnswer(sqliteSql?: string) {
    renderWithProviders(
        <ToastProvider>
            <ProblemStudio problemId={WITH_DATASET_ID} step="answer" />
        </ToastProvider>,
    );
    fireEvent.change(await screen.findByRole("textbox", { name: "Đáp án PostgreSQL" }), {
        target: { value: "SELECT name FROM departments" },
    });
    if (sqliteSql) {
        fireEvent.click(screen.getByRole("checkbox", { name: "Thêm đáp án SQLite" }));
        fireEvent.change(screen.getByRole("textbox", { name: "Đáp án SQLite" }), {
            target: { value: sqliteSql },
        });
    }
    fireEvent.click(screen.getByRole("checkbox", { name: "Mẫu" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "Có NULL" }));
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
    await screen.findByText(/^Đã lưu/);
}

describe("ProblemStudio - kiểm tra đáp án", () => {
    beforeEach(() => {
        writeMockSession(SETTER_SESSION_USER);
    });

    it("đáp án chưa lưu thì chưa kiểm tra được", async () => {
        renderWithProviders(
            <ToastProvider>
                <ProblemStudio problemId={WITH_DATASET_ID} step="answer" />
            </ToastProvider>,
        );

        fireEvent.change(await screen.findByRole("textbox", { name: "Đáp án PostgreSQL" }), {
            target: { value: "SELECT 1" },
        });

        expect(screen.getByRole("button", { name: "Kiểm tra đáp án" })).toBeDisabled();
        expect(screen.getByText("Lưu đáp án và testcase trước khi kiểm tra.")).toBeInTheDocument();
    });

    it("chạy đáp án trên mọi testcase, hiện tiến độ rồi kết quả đạt theo từng dialect", async () => {
        await saveAnswer("SELECT name FROM departments;");

        fireEvent.click(screen.getByRole("button", { name: "Kiểm tra đáp án" }));

        expect(await screen.findByText("Đang kiểm tra: 0/2 testcase")).toBeInTheDocument();
        expect(
            await screen.findByText("Đạt: đáp án chuẩn cho cùng kết quả trên mọi dialect."),
        ).toBeInTheDocument();
        const table = within(screen.getByRole("table", { name: "Kết quả kiểm tra đáp án" }));
        expect(table.getByRole("columnheader", { name: "PostgreSQL" })).toBeInTheDocument();
        expect(table.getByRole("columnheader", { name: "SQLite" })).toBeInTheDocument();
        const rows = table.getAllByRole("row").slice(1);
        expect(rows.map((row) => within(row).getAllByRole("cell")[0].textContent)).toEqual([
            expect.stringContaining("Testcase 1"),
            expect.stringContaining("Testcase 2"),
        ]);
        expect(within(rows[1]).getByText("Ẩn")).toBeInTheDocument();
        expect(within(rows[0]).getAllByText("AC")).toHaveLength(2);
    });

    it("dialect lệch kết quả hoặc lỗi máy chấm thì báo chưa đạt kèm lý do", async () => {
        await saveAnswer("SELECT name FROM departments;");
        server.use(
            http.get("*/api/studio/problems/:problemId/answer-checks/:checkId", ({ params }) =>
                HttpResponse.json({
                    id: params.checkId,
                    status: "FAILED",
                    testcaseCount: 2,
                    checkedCount: 2,
                    createdAt: "2026-11-04T08:00:00Z",
                    completedAt: "2026-11-04T08:00:02Z",
                    testcases: [
                        {
                            testcaseId: "t1",
                            position: 1,
                            isPublic: true,
                            isPassed: false,
                            systemError: null,
                            dialects: [
                                {
                                    dialect: "POSTGRESQL",
                                    verdict: "AC",
                                    mismatch: "NONE",
                                    message: null,
                                    runtimeMs: 3,
                                    rowCount: 1,
                                },
                                {
                                    dialect: "SQLITE",
                                    verdict: "WA",
                                    mismatch: "ROWS",
                                    message: null,
                                    runtimeMs: 1,
                                    rowCount: 2,
                                },
                            ],
                        },
                        {
                            testcaseId: "t2",
                            position: 2,
                            isPublic: false,
                            isPassed: false,
                            systemError: "snapshot_unavailable",
                            dialects: [],
                        },
                    ],
                }),
            ),
        );

        fireEvent.click(screen.getByRole("button", { name: "Kiểm tra đáp án" }));

        expect(
            await screen.findByText(
                "Chưa đạt: sửa đáp án ở các testcase được đánh dấu rồi kiểm tra lại.",
            ),
        ).toBeInTheDocument();
        expect(screen.getByText("WA - Sai dữ liệu")).toBeInTheDocument();
        expect(
            screen.getByText("Máy chấm gặp lỗi khi chạy testcase này. Thử lại sau."),
        ).toBeInTheDocument();
    });
});
