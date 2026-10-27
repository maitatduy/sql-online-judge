import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    CodeRun,
    ProblemDetail,
    RunAccepted,
    StartRunRequest,
} from "@/features/practice/types";
import type { ResultSet } from "@/lib/result-set";

import { validationFailed } from "./admin-user-errors";
import { apiError } from "./api-error";
import { buildProblemSummaries } from "./practice-fixtures";
import { buildProblemDetail } from "./practice-problem-details";
import { readMockSession } from "./session";

const MAX_CODE_BYTES = 64 * 1024;
const MOCK_RUNTIME_MS = 41;
const FORBIDDEN_STATEMENT = /\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|GRANT)\b/i;
const SLOW_QUERY = /\bpg_sleep\b/i;
const MISSPELLED_KEYWORD = /\bFORM\b/i;
const QUERY_START = /^(SELECT|WITH)\b/i;
const FALLBACK_RESULT: ResultSet = { columns: ["?column?"], rows: [["1"]], isTruncated: false };

interface StoredRun {
    userId: string;
    isFirstPoll: boolean;
    run: CodeRun;
}

type RunOutcomeFields = Pick<CodeRun, "outcome" | "result" | "errorMessage" | "runtimeMs">;

const runs = new Map<string, StoredRun>();

function findProblem(problemId: string): ProblemDetail | null {
    const summary = buildProblemSummaries(true).find((problem) => problem.id === problemId);
    return summary ? buildProblemDetail(summary) : null;
}

function syntaxError(token: string): RunOutcomeFields {
    return {
        outcome: "RE",
        result: null,
        errorMessage: `ERROR:  syntax error at or near "${token}"`,
        runtimeMs: null,
    };
}

function outcomeOf(problem: ProblemDetail, code: string): RunOutcomeFields {
    const query = code.trim();
    if (FORBIDDEN_STATEMENT.test(query)) {
        return {
            outcome: "FS",
            result: null,
            errorMessage: `Bài này chỉ cho phép ${problem.allowedStatements.join(", ")}.`,
            runtimeMs: null,
        };
    }
    if (SLOW_QUERY.test(query)) {
        return { outcome: "TLE", result: null, errorMessage: null, runtimeMs: null };
    }
    const misspelled = MISSPELLED_KEYWORD.exec(query);
    if (misspelled) {
        return syntaxError(misspelled[0]);
    }
    if (!QUERY_START.test(query)) {
        return syntaxError(query.split(/\s+/)[0]);
    }
    return {
        outcome: "OK",
        result: problem.examples[0] ?? FALLBACK_RESULT,
        errorMessage: null,
        runtimeMs: MOCK_RUNTIME_MS,
    };
}

function invalidRequest(problem: ProblemDetail, request: StartRunRequest) {
    if (!request.code?.trim()) {
        return validationFailed({ field: "code", message: "Không được để trống" });
    }
    if (new TextEncoder().encode(request.code).length > MAX_CODE_BYTES) {
        return validationFailed({ field: "code", message: "Mã nguồn tối đa 64 KB" });
    }
    if (!problem.dialects.includes(request.dialect)) {
        return validationFailed({
            field: "dialect",
            message: "Bài này không hỗ trợ dialect đã chọn",
        });
    }
    return null;
}

export const practiceRunHandlers: RequestHandler[] = [
    http.post("*/api/problems/:problemId/runs", async ({ params, request }) => {
        await delay();
        const user = readMockSession();
        if (!user) {
            return apiError(401, "UNAUTHENTICATED", "Bạn chưa đăng nhập");
        }
        const problem = findProblem(String(params.problemId));
        if (!problem) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        const body = (await request.json()) as StartRunRequest;
        const invalid = invalidRequest(problem, body);
        if (invalid) {
            return invalid;
        }
        const runId = crypto.randomUUID();
        runs.set(runId, {
            userId: user.id,
            isFirstPoll: true,
            run: { id: runId, status: "COMPLETED", ...outcomeOf(problem, body.code) },
        });
        const response: RunAccepted = { runId };
        return HttpResponse.json(response, { status: 202 });
    }),

    http.get("*/api/runs/:runId", async ({ params }) => {
        await delay();
        const stored = runs.get(String(params.runId));
        if (!stored || stored.userId !== readMockSession()?.id) {
            return apiError(404, "RUN_NOT_FOUND", "Không tìm thấy lần chạy thử");
        }
        if (stored.isFirstPoll) {
            stored.isFirstPoll = false;
            const pending: CodeRun = {
                id: stored.run.id,
                status: "PENDING",
                outcome: null,
                result: null,
                errorMessage: null,
                runtimeMs: null,
            };
            return HttpResponse.json(pending);
        }
        return HttpResponse.json(stored.run);
    }),
];
