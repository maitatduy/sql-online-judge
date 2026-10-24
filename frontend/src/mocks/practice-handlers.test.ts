import { describe, expect, it } from "vitest";

import type { ProblemSummary, TopicsResponse } from "@/features/practice/types";
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
        const page = await listProblems({ difficulty: "HARD", topic: "window-function" });

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

describe("GET /api/problems/topics", () => {
    it("trả danh sách chủ đề có slug và tên hiển thị", async () => {
        const topics = await apiClient.get<TopicsResponse>("/api/problems/topics");

        expect(topics.items).toContainEqual({ slug: "window-function", name: "Window Function" });
        expect(topics.items).toHaveLength(8);
    });
});
