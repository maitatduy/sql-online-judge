import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    FailedTestcase,
    ProblemDetail,
    Submission,
    SubmissionAccepted,
    SubmitSolutionRequest,
} from "@/features/practice/types";
import type { Verdict } from "@/lib/verdict";

import { apiError } from "./api-error";
import { checkQuery, findProblem, invalidQueryRequest } from "./practice-sql";
import { readMockSession } from "./session";

const MOCK_RUNTIME_MS = 48;
const MISSING_ROW_INDEX = 1;
const KEEPS_ONE_ROW_PER_GROUP = /\bDISTINCT\s+ON\b/i;
const COMPARES_WITH_GROUP_MAX = /\b(MAX|RANK|DENSE_RANK)\s*\(/i;
const HAS_ORDER_BY = /\bORDER\s+BY\b/i;

interface Judgement {
    verdict: Verdict;
    judgedTestcases: number;
    firstFailedTestcase: FailedTestcase | null;
    errorMessage: string | null;
}

interface StoredSubmission {
    userId: string;
    pollCount: number;
    result: Submission;
}

const submissions = new Map<string, StoredSubmission>();
const submissionIdsByKey = new Map<string, string>();

export function listOwnSubmissions(userId: string, problemId: string): Submission[] {
    return [...submissions.values()]
        .filter((stored) => stored.userId === userId && stored.result.problemId === problemId)
        .map((stored) => stored.result);
}

function failedAt(testcase: FailedTestcase, verdict: Verdict): Judgement {
    return {
        verdict,
        judgedTestcases: testcase.index,
        firstFailedTestcase: testcase,
        errorMessage: null,
    };
}

function judgeValidQuery(problem: ProblemDetail, query: string, total: number): Judgement {
    const [example] = problem.examples;
    if (example && KEEPS_ONE_ROW_PER_GROUP.test(query)) {
        const actual = {
            ...example,
            rows: example.rows.filter((_, index) => index !== MISSING_ROW_INDEX),
        };
        const diff = { expected: example, actual, missingRows: [MISSING_ROW_INDEX], extraRows: [] };
        return failedAt({ index: 1, isHidden: false, diff }, "WA");
    }
    if (COMPARES_WITH_GROUP_MAX.test(query)) {
        if (example && problem.requiresOrder && !HAS_ORDER_BY.test(query)) {
            const actual = { ...example, rows: [...example.rows].reverse() };
            const diff = { expected: example, actual, missingRows: [], extraRows: [] };
            return failedAt({ index: 1, isHidden: false, diff }, "OM");
        }
        return {
            verdict: "AC",
            judgedTestcases: total,
            firstFailedTestcase: null,
            errorMessage: null,
        };
    }
    return failedAt({ index: problem.examples.length + 1, isHidden: true, diff: null }, "WA");
}

function judge(problem: ProblemDetail, code: string, total: number): Judgement {
    const check = checkQuery(problem, code);
    if (check.outcome === "VALID") {
        return judgeValidQuery(problem, check.query, total);
    }
    if (check.outcome === "TLE") {
        const testcase = { index: 1, isHidden: problem.examples.length === 0, diff: null };
        return failedAt(testcase, "TLE");
    }
    return {
        verdict: check.outcome,
        judgedTestcases: 0,
        firstFailedTestcase: null,
        errorMessage: check.errorMessage,
    };
}

function inProgress(result: Submission, judgedTestcases: number): Submission {
    return {
        ...result,
        status: judgedTestcases === 0 ? "PENDING" : "JUDGING",
        verdict: null,
        judgedTestcases,
        runtimeMs: null,
        errorMessage: null,
        firstFailedTestcase: null,
    };
}

function progressOf(stored: StoredSubmission): Submission {
    const judgedSoFar = stored.pollCount - 1;
    if (judgedSoFar >= stored.result.judgedTestcases && stored.pollCount > 1) {
        return stored.result;
    }
    return inProgress(stored.result, Math.max(judgedSoFar, 0));
}

function sseMessage(event: string, submission: Submission): Uint8Array {
    return new TextEncoder().encode(`event: ${event}
data: ${JSON.stringify(submission)}

`);
}

function judgingStream(result: Submission): ReadableStream<Uint8Array> {
    return new ReadableStream({
        async start(controller) {
            for (let judged = 0; judged < result.judgedTestcases; judged += 1) {
                await delay();
                controller.enqueue(sseMessage("progress", inProgress(result, judged)));
            }
            await delay();
            controller.enqueue(sseMessage("completed", result));
            controller.close();
        },
    });
}

function findOwnSubmission(submissionId: string): StoredSubmission | null {
    const stored = submissions.get(submissionId);
    return stored && stored.userId === readMockSession()?.id ? stored : null;
}

function createSubmission(problem: ProblemDetail, request: SubmitSolutionRequest): Submission {
    const totalTestcases = problem.examples.length + problem.hiddenTestcaseCount;
    const judgement = judge(problem, request.code, totalTestcases);
    return {
        id: crypto.randomUUID(),
        problemId: problem.id,
        dialect: request.dialect,
        status: "COMPLETED",
        totalTestcases,
        runtimeMs:
            judgement.verdict === "RE" || judgement.verdict === "FS" ? null : MOCK_RUNTIME_MS,
        submittedAt: new Date().toISOString(),
        ...judgement,
    };
}

export const practiceSubmissionHandlers: RequestHandler[] = [
    http.post("*/api/problems/:problemId/submissions", async ({ params, request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const problem = findProblem(String(params.problemId));
        if (!problem) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        const body = (await request.json()) as SubmitSolutionRequest;
        const invalid = invalidQueryRequest(problem, body);
        if (invalid) {
            return invalid;
        }
        const idempotencyKey = `${user.id}:${request.headers.get("Idempotency-Key") ?? crypto.randomUUID()}`;
        let submissionId = submissionIdsByKey.get(idempotencyKey);
        if (!submissionId) {
            const result = createSubmission(problem, body);
            submissions.set(result.id, { userId: user.id, pollCount: 0, result });
            submissionIdsByKey.set(idempotencyKey, result.id);
            submissionId = result.id;
        }
        const response: SubmissionAccepted = { submissionId };
        return HttpResponse.json(response, { status: 202 });
    }),

    http.get("*/api/submissions/:submissionId", async ({ params }) => {
        await delay();
        const stored = findOwnSubmission(String(params.submissionId));
        if (!stored) {
            return apiError(404, "SUBMISSION_NOT_FOUND", "Không tìm thấy bài nộp");
        }
        stored.pollCount += 1;
        return HttpResponse.json(progressOf(stored));
    }),

    http.get("*/api/submissions/:submissionId/events", ({ params }) => {
        const stored = findOwnSubmission(String(params.submissionId));
        if (!stored) {
            return apiError(404, "SUBMISSION_NOT_FOUND", "Không tìm thấy bài nộp");
        }
        return new HttpResponse(judgingStream(stored.result), {
            headers: { "Content-Type": "text/event-stream", "Cache-Control": "no-cache" },
        });
    }),
];
