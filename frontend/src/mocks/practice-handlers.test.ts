import { describe, expect, it } from "vitest";

import type { ProblemDetail, ProblemSummary, TagsResponse } from "@/features/practice/types";
import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

import { DEMO_SESSION_USER, writeMockSession } from "./session";

const PROBLEMS_PATH = "/api/problems";

function listProblems(params: Record<string, string | number> = {}) {
    return apiClient.get<Page<ProblemSummary>>(PROBLEMS_PATH, { params });
}

describe("GET /api/problems", () => {
    it("trả trang đầu 20 bài, xếp theo số bài tăng dần", async () => {
        const page = await listProblems({ page: 1, size: 20 });

        expect(page).toMatchObject({ page: 1, size: 20, totalItems: 24, totalPages: 2 });
        expect(page.items).toHaveLength(20);
        const numbers = page.items.map((problem) => problem.number);
        expect(numbers).toEqual([...numbers].sort((first, second) => first - second));
    });

    it("trả trang cuối với phần còn lại", async () => {
        const page = await listProblems({ page: 2, size: 20 });

        expect(page.items).toHaveLength(4);
    });

    it("lọc theo độ khó và chủ đề cùng lúc", async () => {
        const page = await listProblems({ difficulty: "HARD", tag: "window-function" });

        expect(page.items.map((problem) => problem.slug)).toEqual([
            "top-three-products-per-month",
            "booking-gaps",
            "longest-login-streak",
        ]);
    });

    it("tìm theo tên bài không phân biệt hoa thường hoặc theo số bài", async () => {
        const byTitle = await listProblems({ q: "LƯƠNG" });
        const byNumber = await listProblems({ q: "184" });

        expect(byTitle.items.map((problem) => problem.number)).toEqual([184]);
        expect(byNumber.items.map((problem) => problem.number)).toEqual([184]);
    });

    it("không có tiến độ khi chưa đăng nhập", async () => {
        const page = await listProblems();

        expect(page.items.every((problem) => problem.progress === null)).toBe(true);
    });

    it("có tiến độ của người đang đăng nhập và lọc được theo tiến độ", async () => {
        writeMockSession(DEMO_SESSION_USER);

        const solved = await listProblems({ progress: "SOLVED", size: 100 });
        const all = await listProblems({ size: 100 });

        expect(solved.items.map((problem) => problem.number)).toEqual([
            12, 18, 34, 52, 95, 140, 184,
        ]);
        expect(all.items.find((problem) => problem.number === 27)?.progress).toBe("ATTEMPTED");
        expect(all.items.find((problem) => problem.number === 8)?.progress).toBe("NOT_STARTED");
    });

    it.each([
        ["difficulty", { difficulty: "EXTREME" }],
        ["progress", { progress: "DONE" }],
        ["size", { size: 101 }],
        ["page", { page: 0 }],
    ])("trả 400 kèm lỗi ở trường %s", async (field, params) => {
        await expect(listProblems(params)).rejects.toMatchObject({
            status: 400,
            code: "VALIDATION_FAILED",
            errors: [{ field }],
        });
    });
});

describe("GET /api/tags", () => {
    it("trả danh sách chủ đề có slug và tên hiển thị", async () => {
        const tags = await apiClient.get<TagsResponse>("/api/tags");

        expect(tags.items).toContainEqual({ slug: "window-function", name: "Window Function" });
        expect(tags.items).toHaveLength(8);
    });
});

describe("GET /api/problems/{slug}", () => {
    it("trả đề bài, schema, mã khởi đầu và ví dụ của testcase công khai", async () => {
        const problem = await apiClient.get<ProblemDetail>(
            "/api/problems/highest-salary-per-department",
        );

        expect(problem).toMatchObject({
            number: 184,
            title: "Lương cao nhất mỗi phòng ban",
            difficulty: "MEDIUM",
            dialects: ["POSTGRESQL"],
            engineVersions: { POSTGRESQL: "PostgreSQL 16" },
            requiresOrder: true,
            allowedStatements: ["SELECT", "WITH"],
            timeLimitMs: 2000,
            hiddenTestcaseCount: 2,
        });
        expect(problem.statementMarkdown).toContain("**lương cao nhất trong mỗi phòng ban**");
        expect(problem.schema.map((table) => table.name)).toEqual(["departments", "employees"]);
        expect(problem.examples[0].rows).toContainEqual(["Kỹ thuật", "Hoàng Quốc Bảo", "48000000"]);
    });

    it("không trả đáp án chuẩn hay dữ liệu testcase ẩn", async () => {
        const problem = await apiClient.get<ProblemDetail>(
            "/api/problems/customers-who-never-order",
        );

        expect(JSON.stringify(problem)).not.toMatch(
            /datasetScript|expectedResult|referenceSolution/,
        );
    });

    it("trả 404 khi không có bài", async () => {
        await expect(apiClient.get("/api/problems/khong-co-bai-nay")).rejects.toMatchObject({
            status: 404,
            code: "PROBLEM_NOT_FOUND",
        });
    });
});
