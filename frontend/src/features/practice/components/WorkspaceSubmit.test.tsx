import { fireEvent, screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/mocks/server";
import { DEMO_SESSION_USER, writeMockSession } from "@/mocks/session";
import { renderWithProviders } from "@/test/render";

import type { Submission } from "../types";
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
const EXAMPLE_COLUMNS = ["department", "employee", "salary"];
const EXPECTED_ROWS = [
    ["Kỹ thuật", "Hoàng Quốc Bảo", "48000000"],
    ["Kỹ thuật", "Nguyễn Văn An", "48000000"],
    ["Kinh doanh", "Trần Thị Bình", "32000000"],
];

function submissionOf(overrides: Partial<Submission>): Submission {
    return {
        id: "0192f3c0-0000-7000-8000-000000000001",
        problemId: "0192f3b0-0000-7000-8000-0000000000b8",
        dialect: "POSTGRESQL",
        status: "COMPLETED",
        verdict: "AC",
        judgedTestcases: 3,
        totalTestcases: 3,
        runtimeMs: 48,
        errorMessage: null,
        firstFailedTestcase: null,
        submittedAt: "2026-10-27T12:00:00Z",
        ...overrides,
    };
}

const CORRECT_QUERY =
    "SELECT * FROM employees e WHERE e.salary = (SELECT MAX(salary) FROM employees) ORDER BY 1;";
const CONNECTION_LOST_MESSAGE = "Mất kết nối máy chấm, đang thử lại…";

function eventStreamOf(submission: Submission): ReadableStream<Uint8Array> {
    const event = submission.status === "COMPLETED" ? "completed" : "progress";
    const message = `event: ${event}\ndata: ${JSON.stringify(submission)}\n\n`;
    return new ReadableStream({
        start(controller) {
            controller.enqueue(new TextEncoder().encode(message));
            if (event === "completed") {
                controller.close();
            }
        },
    });
}

function judgeAs(submission: Submission) {
    server.use(
        http.get(
            "*/api/submissions/:submissionId/events",
            () =>
                new HttpResponse(eventStreamOf(submission), {
                    headers: { "Content-Type": "text/event-stream" },
                }),
        ),
    );
}

async function submitCode(code?: string) {
    renderWithProviders(<Workspace slug={SLUG} />);
    const editor = await screen.findByRole("textbox", { name: "Mã SQL của bạn" });
    if (code !== undefined) {
        fireEvent.change(editor, { target: { value: code } });
    }
    fireEvent.click(screen.getByRole("button", { name: "Submit" }));
    return within(screen.getByRole("tabpanel", { name: "Nộp bài" }));
}

describe("nộp bài trong Workspace", () => {
    beforeEach(() => {
        localStorage.clear();
        writeMockSession(DEMO_SESSION_USER);
    });

    it("gửi mã kèm Idempotency-Key và hiện tiến độ chấm, khoá Submit khi đang chấm", async () => {
        let idempotencyKey: string | null = null;
        server.use(
            http.post("*/api/problems/:problemId/submissions", async ({ request }) => {
                idempotencyKey = request.headers.get("Idempotency-Key");
                return HttpResponse.json(
                    { submissionId: "0192f3c0-0000-7000-8000-000000000001" },
                    { status: 202 },
                );
            }),
        );
        judgeAs(submissionOf({ status: "JUDGING", verdict: null, judgedTestcases: 1 }));

        const panel = await submitCode();

        expect(await panel.findByText("Đang chấm testcase 2/3…")).toBeInTheDocument();
        expect(panel.getByRole("progressbar", { name: "Tiến độ chấm" })).toHaveAttribute(
            "aria-valuenow",
            "1",
        );
        expect(screen.getByRole("button", { name: "Submit" })).toBeDisabled();
        expect(idempotencyKey).toMatch(/^[0-9a-f-]{36}$/);
    });

    it("hiện AC với số testcase và thời gian chạy", async () => {
        judgeAs(submissionOf({}));

        const panel = await submitCode();

        expect(await panel.findByText("3/3 testcase · 48 ms")).toBeInTheDocument();
        expect(panel.getByText("AC")).toHaveAttribute("title", "Đúng");
        expect(screen.getByRole("button", { name: "Submit" })).toBeEnabled();
    });

    it("so sánh kết quả khi sai ở testcase công khai", async () => {
        judgeAs(
            submissionOf({
                verdict: "WA",
                judgedTestcases: 1,
                runtimeMs: 41,
                firstFailedTestcase: {
                    index: 1,
                    isHidden: false,
                    diff: {
                        expected: {
                            columns: EXAMPLE_COLUMNS,
                            rows: EXPECTED_ROWS,
                            isTruncated: false,
                        },
                        actual: {
                            columns: EXAMPLE_COLUMNS,
                            rows: [EXPECTED_ROWS[0], EXPECTED_ROWS[2]],
                            isTruncated: false,
                        },
                        missingRows: [1],
                        extraRows: [],
                    },
                },
            }),
        );

        const panel = await submitCode();

        expect(
            await panel.findByText("Sai ở testcase 1/3 · thiếu 1 dòng · 41 ms"),
        ).toBeInTheDocument();
        const actual = within(panel.getByRole("table", { name: "Kết quả của bạn · 2 dòng" }));
        expect(actual.getAllByRole("row")).toHaveLength(3);
        const expected = within(panel.getByRole("table", { name: "Mong đợi · 3 dòng" }));
        expect(
            expected.getByRole("row", { name: "Thiếu Kỹ thuật Nguyễn Văn An 48000000" }),
        ).toBeInTheDocument();
    });

    it("không hiện dữ liệu khi sai ở testcase ẩn", async () => {
        judgeAs(
            submissionOf({
                verdict: "WA",
                judgedTestcases: 2,
                firstFailedTestcase: { index: 2, isHidden: true, diff: null },
            }),
        );

        const panel = await submitCode();

        expect(await panel.findByText("Sai ở testcase 2/3 · 48 ms")).toBeInTheDocument();
        expect(panel.getByText("Testcase ẩn nên không hiện dữ liệu so sánh.")).toBeInTheDocument();
        expect(panel.queryByRole("table")).not.toBeInTheDocument();
    });

    it("hiện thông báo lỗi khi RE", async () => {
        judgeAs(
            submissionOf({
                verdict: "RE",
                judgedTestcases: 0,
                runtimeMs: null,
                errorMessage: 'ERROR:  syntax error at or near "FORM"',
            }),
        );

        const panel = await submitCode();

        expect(await panel.findByText(/syntax error at or near "FORM"/)).toBeInTheDocument();
        expect(panel.getByText("RE")).toBeInTheDocument();
    });

    it("mời đăng nhập khi chưa đăng nhập", async () => {
        sessionStorage.clear();

        const panel = await submitCode();

        expect(await panel.findByText("Đăng nhập để nộp bài.")).toBeInTheDocument();
    });

    it("báo thời gian chờ khi nộp quá nhanh", async () => {
        server.use(
            http.post("*/api/problems/:problemId/submissions", () =>
                HttpResponse.json(
                    { status: 429, code: "SUBMISSION_RATE_LIMITED", message: "Nộp quá nhanh" },
                    { status: 429, headers: { "Retry-After": "30" } },
                ),
            ),
        );

        const panel = await submitCode();

        expect(
            await panel.findByText("Bạn nộp bài quá nhanh. Thử lại sau 30 giây."),
        ).toBeInTheDocument();
    });

    it("nhận tiến độ và verdict qua SSE mà không hỏi lại máy chủ", async () => {
        let pollCount = 0;
        server.events.on("request:start", ({ request }) => {
            if (/\/api\/submissions\/[^/]+$/.test(new URL(request.url).pathname)) {
                pollCount += 1;
            }
        });

        const panel = await submitCode(CORRECT_QUERY);

        expect(await panel.findByText("3/3 testcase · 48 ms")).toBeInTheDocument();
        expect(pollCount).toBe(0);
        expect(screen.queryByText(CONNECTION_LOST_MESSAGE)).not.toBeInTheDocument();
        server.events.removeAllListeners();
    });

    it("báo mất kết nối và chuyển sang hỏi lại khi luồng SSE lỗi", async () => {
        server.use(
            http.get("*/api/submissions/:submissionId/events", () => HttpResponse.error()),
            http.get("*/api/submissions/:submissionId", () => HttpResponse.json(submissionOf({}))),
        );

        const panel = await submitCode();

        expect(await screen.findByText(CONNECTION_LOST_MESSAGE)).toBeInTheDocument();
        expect(await panel.findByText("3/3 testcase · 48 ms")).toBeInTheDocument();
    });
});
