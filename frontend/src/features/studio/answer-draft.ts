import type { Dialect } from "@/lib/dialect";

import type { ProblemAnswer, SaveProblemAnswerRequest } from "./types";

export const ANSWER_DIALECTS: readonly Dialect[] = ["POSTGRESQL", "MYSQL", "SQLITE"];
export const REQUIRED_DIALECT: Dialect = "POSTGRESQL";

export interface AnswerDraft {
    sqlByDialect: Record<Dialect, string>;
    enabledDialects: Dialect[];
    testcaseIds: string[];
    requiresOrder: boolean;
    compareColumnNames: boolean;
    floatTolerance: string;
}

export function draftFrom(answer: ProblemAnswer): AnswerDraft {
    const sqlByDialect: Record<Dialect, string> = { POSTGRESQL: "", MYSQL: "", SQLITE: "" };
    answer.solutions.forEach((solution) => {
        sqlByDialect[solution.dialect] = solution.sql;
    });
    const enabled = new Set<Dialect>([
        REQUIRED_DIALECT,
        ...answer.solutions.map((solution) => solution.dialect),
    ]);
    return {
        sqlByDialect,
        enabledDialects: ANSWER_DIALECTS.filter((dialect) => enabled.has(dialect)),
        testcaseIds: answer.testcases.map((testcase) => testcase.variantId),
        requiresOrder: answer.requiresOrder,
        compareColumnNames: answer.compareColumnNames,
        floatTolerance: String(answer.floatTolerance),
    };
}

export function requestFrom(draft: AnswerDraft): SaveProblemAnswerRequest {
    return {
        solutions: draft.enabledDialects.map((dialect) => ({
            dialect,
            sql: draft.sqlByDialect[dialect],
        })),
        testcaseVariantIds: draft.testcaseIds,
        requiresOrder: draft.requiresOrder,
        compareColumnNames: draft.compareColumnNames,
        floatTolerance: Number(draft.floatTolerance),
    };
}

export function isToleranceValid(floatTolerance: string): boolean {
    const value = Number(floatTolerance);
    return floatTolerance.trim() !== "" && Number.isFinite(value) && value >= 0 && value <= 1;
}

export function isDraftComplete(draft: AnswerDraft): boolean {
    return (
        draft.enabledDialects.every((dialect) => draft.sqlByDialect[dialect].trim() !== "") &&
        draft.testcaseIds.length > 0 &&
        isToleranceValid(draft.floatTolerance)
    );
}
