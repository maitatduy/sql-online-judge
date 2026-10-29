import { beforeEach, describe, expect, it } from "vitest";

import type { CodeRun, RunAccepted } from "@/features/practice/types";
import { apiClient } from "@/lib/api-client";

import { DEMO_SESSION_USER, writeMockSession } from "./session";

const PROBLEM_ID = "0192f3b0-0000-7000-8000-0000000000b8";

function startRun(code: string, dialect = "POSTGRESQL", problemId = PROBLEM_ID) {
    return apiClient.post<RunAccepted>(`/api/problems/${problemId}/runs`, { dialect, code });
}

function getRun(runId: string) {
    return apiClient.get<CodeRun>(`/api/runs/${runId}`);
}

async function completedRun(code: string): Promise<CodeRun> {
    const { runId } = await startRun(code);
    await getRun(runId);
    return getRun(runId);
}

describe("chạy thử", () => {
    beforeEach(() => {
        writeMockSession(DEMO_SESSION_USER);
    });

    it("nhận bài chạy thử rồi trả PENDING trước khi có kết quả", async () => {
        const { runId } = await startRun("SELECT * FROM employees;");

        expect(runId).toMatch(/^[0-9a-f-]{36}$/);
        expect(await getRun(runId)).toMatchObject({ id: runId, status: "PENDING", outcome: null });
    });

    it("trả bảng kết quả trên dữ liệu mẫu khi câu truy vấn hợp lệ", async () => {
        const run = await completedRun("SELECT * FROM employees;");

        expect(run).toMatchObject({ status: "COMPLETED", outcome: "OK", errorMessage: null });
        expect(run.result?.columns).toEqual(["department", "employee", "salary"]);
        expect(run.runtimeMs).toBeGreaterThan(0);
    });

    it("trả RE kèm thông báo lỗi cú pháp", async () => {
        const run = await completedRun("SELECT * FORM employees;");

        expect(run).toMatchObject({ outcome: "RE", result: null });
        expect(run.errorMessage).toContain('syntax error at or near "FORM"');
    });

    it("trả FS khi dùng lệnh ngoài danh sách cho phép", async () => {
        const run = await completedRun("DROP TABLE employees;");

        expect(run).toMatchObject({
            outcome: "FS",
            errorMessage: "Bài này chỉ cho phép SELECT, WITH.",
        });
    });

    it("trả TLE khi câu truy vấn chạy quá giới hạn", async () => {
        const run = await completedRun("SELECT pg_sleep(60);");

        expect(run).toMatchObject({ outcome: "TLE", result: null });
    });

    it("từ chối mã trống hoặc dialect bài không hỗ trợ", async () => {
        await expect(startRun("   ")).rejects.toMatchObject({
            status: 400,
            errors: [{ field: "code", message: "Không được để trống" }],
        });
        await expect(startRun("SELECT 1;", "MYSQL")).rejects.toMatchObject({
            status: 400,
            errors: [{ field: "dialect", message: "Bài này không hỗ trợ dialect đã chọn" }],
        });
    });

    it("trả 404 khi bài hoặc lần chạy không tồn tại", async () => {
        await expect(
            startRun("SELECT 1;", "POSTGRESQL", "0192f3b0-0000-7000-8000-ffffffffffff"),
        ).rejects.toMatchObject({ status: 404, code: "PROBLEM_NOT_FOUND" });
        await expect(getRun("0192f3b0-0000-7000-8000-ffffffffffff")).rejects.toMatchObject({
            status: 404,
            code: "RUN_NOT_FOUND",
        });
    });

    it("yêu cầu đăng nhập", async () => {
        sessionStorage.clear();

        await expect(startRun("SELECT 1;")).rejects.toMatchObject({ status: 401 });
    });
});
