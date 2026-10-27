import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    CodeRun,
    ProblemDetail,
    RunAccepted,
    StartRunRequest,
} from "@/features/practice/types";
import type { ResultSet } from "@/lib/result-set";

import { apiError } from "./api-error";
import { checkQuery, findProblem, invalidQueryRequest } from "./practice-sql";
import { readMockSession } from "./session";

const MOCK_RUNTIME_MS = 41;
const FALLBACK_RESULT: ResultSet = { columns: ["?column?"], rows: [["1"]], isTruncated: false };

interface StoredRun {
    userId: string;
    isFirstPoll: boolean;
    run: CodeRun;
}

type RunOutcomeFields = Pick<CodeRun, "outcome" | "result" | "errorMessage" | "runtimeMs">;

const runs = new Map<string, StoredRun>();

function outcomeOf(problem: ProblemDetail, code: string): RunOutcomeFields {
    const check = checkQuery(problem, code);
    if (check.outcome === "VALID") {
        return {
            outcome: "OK",
            result: problem.examples[0] ?? FALLBACK_RESULT,
            errorMessage: null,
            runtimeMs: MOCK_RUNTIME_MS,
        };
    }
    return {
        outcome: check.outcome,
        result: null,
        errorMessage: check.outcome === "TLE" ? null : check.errorMessage,
        runtimeMs: null,
    };
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
        const invalid = invalidQueryRequest(problem, body);
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
