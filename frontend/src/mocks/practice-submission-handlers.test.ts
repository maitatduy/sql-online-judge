import { beforeEach, describe, expect, it } from "vitest";

import type { Submission, SubmissionAccepted } from "@/features/practice/types";
import { apiClient } from "@/lib/api-client";
import { type ServerSentEvent, streamEvents } from "@/lib/sse";

import { DEMO_SESSION_USER, writeMockSession } from "./session";

const PROBLEM_ID = "0192f3b0-0000-7000-8000-0000000000b8";
const CORRECT_QUERY = [
    "SELECT d.name AS department, e.full_name AS employee, e.salary",
    "FROM employees e JOIN departments d ON d.id = e.department_id",
    "WHERE e.salary = (SELECT MAX(salary) FROM employees WHERE department_id = e.department_id)",
    "ORDER BY e.salary DESC, employee;",
].join("\n");

function submit(code: string, idempotencyKey = crypto.randomUUID()) {
    return apiClient.post<SubmissionAccepted>(
        `/api/problems/${PROBLEM_ID}/submissions`,
        { dialect: "POSTGRESQL", code },
        { headers: { "Idempotency-Key": idempotencyKey } },
    );
}

function getSubmission(submissionId: string) {
    return apiClient.get<Submission>(`/api/submissions/${submissionId}`);
}

async function judged(code: string): Promise<Submission> {
    const { submissionId } = await submit(code);
    let submission = await getSubmission(submissionId);
    while (submission.status !== "COMPLETED") {
        submission = await getSubmission(submissionId);
    }
    return submission;
}

describe("nộp bài", () => {
    beforeEach(() => {
        writeMockSession(DEMO_SESSION_USER);
    });

    it("chấm lần lượt từng testcase rồi trả AC khi đúng", async () => {
        const { submissionId } = await submit(CORRECT_QUERY);

        expect(await getSubmission(submissionId)).toMatchObject({
            status: "PENDING",
            verdict: null,
            judgedTestcases: 0,
            totalTestcases: 3,
        });
        expect(await getSubmission(submissionId)).toMatchObject({
            status: "JUDGING",
            judgedTestcases: 1,
        });
        expect(await getSubmission(submissionId)).toMatchObject({
            status: "JUDGING",
            judgedTestcases: 2,
        });
        expect(await getSubmission(submissionId)).toMatchObject({
            status: "COMPLETED",
            verdict: "AC",
            judgedTestcases: 3,
            firstFailedTestcase: null,
        });
    });

    it("giữ nguyên bài nộp khi gửi lại cùng Idempotency-Key", async () => {
        const first = await submit(CORRECT_QUERY, "same-key");
        const second = await submit(CORRECT_QUERY, "same-key");

        expect(second.submissionId).toBe(first.submissionId);
    });

    it("trả WA kèm diff khi sai ở testcase công khai", async () => {
        const submission = await judged("SELECT DISTINCT ON (d.name) * FROM employees;");

        expect(submission).toMatchObject({
            verdict: "WA",
            firstFailedTestcase: { index: 1, isHidden: false },
        });
        const diff = submission.firstFailedTestcase?.diff;
        expect(diff?.expected.rows).toHaveLength(4);
        expect(diff?.actual.rows).toHaveLength(3);
        expect(diff?.missingRows).toEqual([1]);
        expect(diff?.extraRows).toEqual([]);
    });

    it("không trả dữ liệu khi sai ở testcase ẩn", async () => {
        const submission = await judged("SELECT * FROM employees;");

        expect(submission).toMatchObject({
            verdict: "WA",
            firstFailedTestcase: { index: 2, isHidden: true, diff: null },
        });
    });

    it("trả OM khi đúng dữ liệu nhưng thiếu ORDER BY", async () => {
        const submission = await judged(CORRECT_QUERY.replace(/ORDER BY.*$/, ";"));

        expect(submission).toMatchObject({
            verdict: "OM",
            firstFailedTestcase: { index: 1, isHidden: false },
        });
    });

    it("trả RE hoặc FS ngay, không chạy testcase nào", async () => {
        expect(await judged("SELECT * FORM employees;")).toMatchObject({
            verdict: "RE",
            judgedTestcases: 0,
            errorMessage: 'ERROR:  syntax error at or near "FORM"',
        });
        expect(await judged("DELETE FROM employees;")).toMatchObject({
            verdict: "FS",
            errorMessage: "Bài này chỉ cho phép SELECT, WITH.",
        });
    });

    it("từ chối mã trống và yêu cầu đăng nhập", async () => {
        await expect(submit(" ")).rejects.toMatchObject({ status: 400 });

        sessionStorage.clear();

        await expect(submit(CORRECT_QUERY)).rejects.toMatchObject({ status: 401 });
    });

    it("trả 404 khi bài nộp không tồn tại hoặc thuộc người khác", async () => {
        await expect(getSubmission("0192f3b0-0000-7000-8000-ffffffffffff")).rejects.toMatchObject({
            status: 404,
            code: "SUBMISSION_NOT_FOUND",
        });
    });

    it("phát tiến độ chấm qua SSE rồi đóng luồng sau sự kiện completed", async () => {
        const { submissionId } = await submit(CORRECT_QUERY);
        const events: ServerSentEvent[] = [];

        await streamEvents(`/api/submissions/${submissionId}/events`, {
            onEvent: (event) => events.push(event),
        });

        expect(events.map((event) => event.event)).toEqual([
            "progress",
            "progress",
            "progress",
            "completed",
        ]);
        const judged = events.map(
            (event) => (JSON.parse(event.data) as Submission).judgedTestcases,
        );
        expect(judged).toEqual([0, 1, 2, 3]);
        expect(JSON.parse(events[3].data)).toMatchObject({ status: "COMPLETED", verdict: "AC" });
    });

    it("không mở luồng SSE cho bài nộp của người khác", async () => {
        await expect(
            streamEvents("/api/submissions/0192f3b0-0000-7000-8000-ffffffffffff/events", {
                onEvent: () => undefined,
            }),
        ).rejects.toMatchObject({ status: 404, code: "SUBMISSION_NOT_FOUND" });
    });
});
