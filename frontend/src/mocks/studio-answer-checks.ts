import { delay, http, HttpResponse, type RequestHandler } from "msw";

import type {
    AnswerCheck,
    AnswerCheckTestcase,
    DialectCheckResult,
    SaveProblemAnswerRequest,
} from "@/features/studio/types";

import { apiError } from "./api-error";
import { readStudioDatasets } from "./studio-datasets";
import { findAccessibleStudioProblem } from "./studio-problems";

const MOCK_RUNTIME_MS = 4;
const MOCK_ROW_COUNT = 1;

interface MockCheck {
    check: AnswerCheck;
    problemId: string;
    completed: AnswerCheck;
    polls: number;
}

const checks = new Map<string, MockCheck>();

function canonicalSql(sql: string): string {
    return sql.trim().replace(/;$/, "").toLowerCase();
}

function dialectResultsOf(answer: SaveProblemAnswerRequest): DialectCheckResult[] {
    const reference = answer.solutions.find((solution) => solution.dialect === "POSTGRESQL");
    return answer.solutions.map((solution) => {
        const matches = canonicalSql(solution.sql) === canonicalSql(reference?.sql ?? "");
        return {
            dialect: solution.dialect,
            verdict: matches ? "AC" : "WA",
            mismatch: matches ? "NONE" : "ROWS",
            message: null,
            runtimeMs: MOCK_RUNTIME_MS,
            rowCount: MOCK_ROW_COUNT,
        };
    });
}

function completedOf(
    check: AnswerCheck,
    answer: SaveProblemAnswerRequest,
    datasetId: string,
): AnswerCheck {
    const variants =
        readStudioDatasets().find((dataset) => dataset.id === datasetId)?.variants ?? [];
    const testcases: AnswerCheckTestcase[] = answer.testcaseVariantIds.map((variantId, index) => {
        const dialects = dialectResultsOf(answer);
        return {
            testcaseId: `${check.id}-${index + 1}`,
            position: index + 1,
            isPublic: variants.find((variant) => variant.id === variantId)?.isPublic ?? false,
            isPassed: dialects.every((result) => result.verdict === "AC"),
            systemError: null,
            dialects,
        };
    });
    return {
        ...check,
        status: testcases.every((testcase) => testcase.isPassed) ? "PASSED" : "FAILED",
        checkedCount: testcases.length,
        completedAt: new Date().toISOString(),
        testcases,
    };
}

export const studioAnswerCheckHandlers: RequestHandler[] = [
    http.post("*/api/studio/problems/:problemId/answer-checks", async ({ params }) => {
        await delay();
        const problemId = String(params.problemId);
        const problem = findAccessibleStudioProblem(problemId);
        if (!problem) {
            return apiError(404, "PROBLEM_NOT_FOUND", "Không tìm thấy bài tập");
        }
        if (!problem.datasetId) {
            return apiError(422, "PROBLEM_DATASET_MISSING", "Bài chưa chọn database");
        }
        const answer = problem.answer;
        if (!answer || answer.testcaseVariantIds.length === 0) {
            return apiError(
                422,
                "PROBLEM_ANSWER_INCOMPLETE",
                "Bài chưa có đáp án chuẩn hoặc testcase",
            );
        }
        const check: AnswerCheck = {
            id: crypto.randomUUID(),
            status: "PENDING",
            testcaseCount: answer.testcaseVariantIds.length,
            checkedCount: 0,
            createdAt: new Date().toISOString(),
            completedAt: null,
            testcases: [],
        };
        checks.set(check.id, {
            check,
            problemId,
            completed: completedOf(check, answer, problem.datasetId),
            polls: 0,
        });
        return HttpResponse.json(check, {
            status: 202,
            headers: { Location: `/api/studio/problems/${problemId}/answer-checks/${check.id}` },
        });
    }),

    http.get("*/api/studio/problems/:problemId/answer-checks/:checkId", async ({ params }) => {
        await delay();
        const mockCheck = checks.get(String(params.checkId));
        if (!mockCheck || mockCheck.problemId !== String(params.problemId)) {
            return apiError(404, "ANSWER_CHECK_NOT_FOUND", "Không tìm thấy lần kiểm tra đáp án");
        }
        if (mockCheck.polls === 0) {
            mockCheck.polls += 1;
            return HttpResponse.json(mockCheck.check);
        }
        return HttpResponse.json(mockCheck.completed);
    }),
];
