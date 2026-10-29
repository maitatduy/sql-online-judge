import { beforeEach, describe, expect, it } from "vitest";

import type { ProblemSubmissionsResponse } from "@/features/practice/types";
import { apiClient } from "@/lib/api-client";

import { ADMIN_SESSION_USER, DEMO_SESSION_USER, writeMockSession } from "./session";

const PROBLEM_ID = "0192f3b0-0000-7000-8000-0000000000b8";
const OTHER_PROBLEM_ID = "0192f3b0-0000-7000-8000-00000000000c";

function listSubmissions(problemId = PROBLEM_ID, params: Record<string, string | number> = {}) {
    return apiClient.get<ProblemSubmissionsResponse>(`/api/problems/${problemId}/submissions`, {
        params,
    });
}

describe("lịch sử nộp của một bài", () => {
    beforeEach(() => {
        writeMockSession(DEMO_SESSION_USER);
    });

    it("trả bài nộp của chính người dùng, mới nhất trước", async () => {
        const page = await listSubmissions();

        expect(page.items.map((item) => item.verdict)).toEqual(["WA", "RE"]);
        expect(page.items[0]).toMatchObject({
            status: "COMPLETED",
            dialect: "POSTGRESQL",
            runtimeMs: 41,
        });
        expect(page.nextCursor).toBeNull();
    });

    it("thêm bài vừa nộp vào đầu danh sách", async () => {
        await apiClient.post(
            `/api/problems/${PROBLEM_ID}/submissions`,
            { dialect: "POSTGRESQL", code: "SELECT * FROM employees;" },
            { headers: { "Idempotency-Key": crypto.randomUUID() } },
        );

        const page = await listSubmissions();

        expect(page.items).toHaveLength(3);
        expect(page.items[0].verdict).toBe("WA");
        expect(Date.parse(page.items[0].submittedAt)).toBeGreaterThan(
            Date.parse(page.items[1].submittedAt),
        );
    });

    it("phân trang bằng cursor", async () => {
        const firstPage = await listSubmissions(PROBLEM_ID, { size: 1 });
        const secondPage = await listSubmissions(PROBLEM_ID, {
            size: 1,
            cursor: firstPage.nextCursor ?? "",
        });

        expect(firstPage.items).toHaveLength(1);
        expect(secondPage.items[0].id).not.toBe(firstPage.items[0].id);
    });

    it("không trả bài nộp của người khác hoặc của bài khác", async () => {
        expect((await listSubmissions(OTHER_PROBLEM_ID)).items).toEqual([]);

        writeMockSession(ADMIN_SESSION_USER);

        expect((await listSubmissions()).items).toEqual([]);
    });

    it("trả 404 khi bài không tồn tại và 401 khi chưa đăng nhập", async () => {
        await expect(listSubmissions("0192f3b0-0000-7000-8000-ffffffffffff")).rejects.toMatchObject(
            { status: 404, code: "PROBLEM_NOT_FOUND" },
        );

        sessionStorage.clear();

        await expect(listSubmissions()).rejects.toMatchObject({ status: 401 });
    });
});
