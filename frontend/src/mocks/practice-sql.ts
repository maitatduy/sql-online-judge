import type { ProblemDetail, StartRunRequest } from "@/features/practice/types";

import { validationFailed } from "./admin-user-errors";
import { buildProblemSummaries } from "./practice-fixtures";
import { buildProblemDetail } from "./practice-problem-details";

const MAX_CODE_BYTES = 64 * 1024;
const FORBIDDEN_STATEMENT = /\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|GRANT)\b/i;
const SLOW_QUERY = /\bpg_sleep\b/i;
const MISSPELLED_KEYWORD = /\bFORM\b/i;
const QUERY_START = /^(SELECT|WITH)\b/i;

export type QueryCheck =
    | { outcome: "FS"; errorMessage: string }
    | { outcome: "RE"; errorMessage: string }
    | { outcome: "TLE" }
    | { outcome: "VALID"; query: string };

export function findProblem(problemId: string): ProblemDetail | null {
    const summary = buildProblemSummaries(true).find((problem) => problem.id === problemId);
    return summary ? buildProblemDetail(summary) : null;
}

export function invalidQueryRequest(problem: ProblemDetail, request: StartRunRequest) {
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

function syntaxError(token: string): QueryCheck {
    return { outcome: "RE", errorMessage: `ERROR:  syntax error at or near "${token}"` };
}

export function checkQuery(problem: ProblemDetail, code: string): QueryCheck {
    const query = code.trim();
    if (FORBIDDEN_STATEMENT.test(query)) {
        return {
            outcome: "FS",
            errorMessage: `Bài này chỉ cho phép ${problem.allowedStatements.join(", ")}.`,
        };
    }
    if (SLOW_QUERY.test(query)) {
        return { outcome: "TLE" };
    }
    const misspelled = MISSPELLED_KEYWORD.exec(query);
    if (misspelled) {
        return syntaxError(misspelled[0]);
    }
    if (!QUERY_START.test(query)) {
        return syntaxError(query.split(/\s+/)[0]);
    }
    return { outcome: "VALID", query };
}
