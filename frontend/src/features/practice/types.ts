import type { Dialect } from "@/lib/dialect";
import type { Difficulty } from "@/lib/difficulty";
import type { ResultSet } from "@/lib/result-set";

export type ProblemProgress = "SOLVED" | "ATTEMPTED" | "NOT_STARTED";

export interface Tag {
    slug: string;
    name: string;
}

export interface ProblemSummary {
    id: string;
    number: number;
    slug: string;
    title: string;
    difficulty: Difficulty;
    tags: Tag[];
    acceptanceRate: number;
    solvedCount: number;
    progress: ProblemProgress | null;
}

export interface TagsResponse {
    items: Tag[];
}

export type StatementType = "SELECT" | "WITH";

export interface SchemaColumn {
    name: string;
    type: string;
    isPrimaryKey: boolean;
    isNullable: boolean;
    references: { table: string; column: string } | null;
}

export interface SchemaTable {
    name: string;
    columns: SchemaColumn[];
}

export interface ProblemDetail {
    id: string;
    number: number;
    slug: string;
    title: string;
    difficulty: Difficulty;
    tags: Tag[];
    statementMarkdown: string;
    dialects: Dialect[];
    engineVersions: Partial<Record<Dialect, string>>;
    starterCode: string;
    schema: SchemaTable[];
    examples: ResultSet[];
    requiresOrder: boolean;
    allowedStatements: StatementType[];
    timeLimitMs: number;
    hiddenTestcaseCount: number;
    acceptanceRate: number;
    solvedCount: number;
}

export type RunStatus = "PENDING" | "COMPLETED";

export type RunOutcome = "OK" | "RE" | "TLE" | "FS";

export interface StartRunRequest {
    dialect: Dialect;
    code: string;
}

export interface RunAccepted {
    runId: string;
}

export interface CodeRun {
    id: string;
    status: RunStatus;
    outcome: RunOutcome | null;
    result: ResultSet | null;
    errorMessage: string | null;
    runtimeMs: number | null;
}
