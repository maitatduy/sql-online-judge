import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type { AdminSubmission, AdminSubmissionDetail } from "@/features/admin/submissions/types";
import type { Dialect } from "@/lib/dialect";
import type { Verdict } from "@/lib/verdict";

import { apiError } from "./api-error";
import { readMockSession } from "./session";

const DEFAULT_PAGE_SIZE = 20;
const MINUTE_MS = 60_000;
const USERS = [
    { userId: "0192f3a0-0000-7000-8000-000000000001", username: "minh.anh" },
    { userId: "0192f3a0-0000-7000-8000-0000000000a2", username: "khoa.tran" },
    { userId: "0192f3a0-0000-7000-8000-0000000000a3", username: "lan.pham" },
];
const PROBLEMS = [
    { number: 184, slug: "highest-salary-per-department", title: "Lương cao nhất mỗi phòng ban" },
    { number: 12, slug: "second-highest-salary", title: "Lương cao thứ hai" },
    { number: 57, slug: "monthly-revenue", title: "Doanh thu theo tháng" },
];
const RESULTS: (Verdict | null)[] = ["AC", "WA", "AC", "RE", "TLE", null, "OM", "SE", "AC", "FS"];
const DIALECTS: Dialect[] = ["POSTGRESQL", "MYSQL", "SQLITE"];
const SUBMISSION_COUNT = 27;

function buildSubmissions(now: Date): AdminSubmissionDetail[] {
    return Array.from({ length: SUBMISSION_COUNT }, (_, index) => {
        const user = USERS[index % USERS.length];
        const problem = PROBLEMS[index % PROBLEMS.length];
        const verdict = RESULTS[index % RESULTS.length];
        const isContest = index % 4 === 3;
        return {
            id: `0193c000-0000-7000-8000-${String(index).padStart(12, "0")}`,
            userId: user.userId,
            username: user.username,
            displayName: null,
            problemId: `0193d000-0000-7000-8000-00000000000${index % PROBLEMS.length}`,
            problemNumber: problem.number,
            problemSlug: problem.slug,
            problemTitle: problem.title,
            contestId: isContest ? "0193b000-0000-7000-8000-000000000001" : null,
            dialect: DIALECTS[index % DIALECTS.length],
            status: verdict ? "COMPLETED" : "PENDING",
            verdict,
            runtimeMs: verdict ? 20 + index * 7 : null,
            createdAt: new Date(now.getTime() - index * 37 * MINUTE_MS).toISOString(),
            code: `SELECT department, MAX(salary)\nFROM employees\nGROUP BY department;`,
            errorMessage: verdict === "RE" ? 'column "salry" does not exist' : null,
            firstFailedIndex: verdict && verdict !== "AC" ? 2 : null,
            judgedTestcases: verdict ? 3 : 0,
            totalTestcases: 3,
            attempts: 1,
            judgedAt: verdict
                ? new Date(now.getTime() - index * 37 * MINUTE_MS).toISOString()
                : null,
        };
    });
}

function toRow(submission: AdminSubmissionDetail): AdminSubmission {
    return {
        id: submission.id,
        userId: submission.userId,
        username: submission.username,
        displayName: submission.displayName,
        problemId: submission.problemId,
        problemNumber: submission.problemNumber,
        problemSlug: submission.problemSlug,
        problemTitle: submission.problemTitle,
        contestId: submission.contestId,
        dialect: submission.dialect,
        status: submission.status,
        verdict: submission.verdict,
        runtimeMs: submission.runtimeMs,
        createdAt: submission.createdAt,
    };
}

function isAdmin(): boolean {
    return readMockSession()?.role === "ADMIN";
}

function forbidden() {
    return apiError(403, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này");
}

function matches(submission: AdminSubmissionDetail, query: URLSearchParams): boolean {
    const source = query.get("source");
    return (
        (!query.get("verdict") || submission.verdict === query.get("verdict")) &&
        (!query.get("status") || submission.status === query.get("status")) &&
        (!query.get("dialect") || submission.dialect === query.get("dialect")) &&
        (!query.get("username") || submission.username === query.get("username")) &&
        (!query.get("problem") || submission.problemSlug === query.get("problem")) &&
        (!source || (source === "CONTEST") === (submission.contestId !== null))
    );
}

export const adminSubmissionHandlers: RequestHandler[] = [
    http.get("*/api/admin/submissions", async ({ request }) => {
        await delay();
        if (!isAdmin()) {
            return forbidden();
        }
        const query = new URL(request.url).searchParams;
        const page = Number(query.get("page") ?? 1);
        const size = Number(query.get("size") ?? DEFAULT_PAGE_SIZE);
        const matching = buildSubmissions(new Date()).filter((submission) =>
            matches(submission, query),
        );
        const items = matching.slice((page - 1) * size, page * size).map(toRow);
        return HttpResponse.json({
            items,
            page,
            size,
            totalItems: matching.length,
            totalPages: Math.ceil(matching.length / size),
        });
    }),

    http.get("*/api/admin/submissions/:submissionId", async ({ params }) => {
        await delay();
        if (!isAdmin()) {
            return forbidden();
        }
        const submission = buildSubmissions(new Date()).find(
            (candidate) => candidate.id === params.submissionId,
        );
        return submission
            ? HttpResponse.json(submission)
            : apiError(404, "SUBMISSION_NOT_FOUND", "Không tìm thấy bài nộp");
    }),
];
