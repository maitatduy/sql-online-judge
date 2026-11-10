import type { Dialect } from "@/lib/dialect";
import type { Difficulty } from "@/lib/difficulty";
import type { Verdict } from "@/lib/verdict";

export type ProblemStatus =
    "DRAFT" | "PENDING_REVIEW" | "CHANGES_REQUESTED" | "REJECTED" | "PUBLISHED";

export interface StudioProblemSummary {
    id: string;
    number: number | null;
    slug: string;
    title: string;
    status: ProblemStatus;
    difficulty: Difficulty;
    updatedAt: string;
}

export interface StudioTag {
    slug: string;
    name: string;
}

export interface StudioProblem {
    id: string;
    number: number | null;
    slug: string;
    title: string;
    status: ProblemStatus;
    difficulty: Difficulty;
    tags: StudioTag[];
    statementMarkdown: string;
    dataset: { id: string; name: string } | null;
    latestReview: ProblemReview | null;
    updatedAt: string;
}

export interface UpdateProblemRequest {
    title?: string;
    slug?: string;
    difficulty?: Difficulty;
    tagSlugs?: string[];
    statementMarkdown?: string;
}

export interface StudioDatasetSummary {
    id: string;
    slug: string;
    name: string;
    description: string;
    updatedAt: string;
}

export type LogicalType = "INTEGER" | "DECIMAL" | "TEXT" | "DATE" | "TIMESTAMP" | "BOOLEAN";

export interface DatasetColumn {
    name: string;
    type: LogicalType;
    isPrimaryKey: boolean;
    isNullable: boolean;
    references: { table: string; column: string } | null;
}

export interface DatasetTable {
    name: string;
    columns: DatasetColumn[];
}

export interface DatasetVariant {
    id: string;
    name: string;
    isPublic: boolean;
    tables: { tableName: string; rowCount: number }[];
}

export interface StudioDataset {
    id: string;
    slug: string;
    name: string;
    description: string;
    versions: { number: number; frozenAt: string | null }[];
    schema: { versionNumber: number; isDraft: boolean; tables: DatasetTable[] };
    variants: DatasetVariant[];
    updatedAt: string;
}

export interface SqlImportPreview {
    tables: DatasetTable[];
    data: { tableName: string; csv: string; rowCount: number }[];
}

export interface PendingImport {
    data: SqlImportPreview["data"];
    variantName: string;
    createsVariant: boolean;
}

export interface VariantCsv {
    tableName: string;
    csv: string;
    rowCount: number;
}

export interface DatasetVariantRequest {
    name: string;
    isPublic: boolean;
}

export interface UpdateDatasetRequest {
    name?: string;
    description?: string;
}

export type StudioStep = "info" | "statement" | "database" | "answer" | "publish";

export interface ReferenceSolution {
    dialect: Dialect;
    sql: string;
}

export interface AnswerVariant {
    id: string;
    name: string;
    isPublic: boolean;
}

export interface ProblemAnswer {
    solutions: ReferenceSolution[];
    testcases: { variantId: string; variantName: string; isPublic: boolean }[];
    variants: AnswerVariant[];
    requiresOrder: boolean;
    compareColumnNames: boolean;
    floatTolerance: number;
}

export interface SaveProblemAnswerRequest {
    solutions: ReferenceSolution[];
    testcaseVariantIds: string[];
    requiresOrder: boolean;
    compareColumnNames: boolean;
    floatTolerance: number;
}

export type StudioRunStatus = "PENDING" | "COMPLETED";

export type VariantRunStatus = "SUCCEEDED" | "FAILED" | "TIMED_OUT" | "SANDBOX_FAILED";

export type RunCell = string | number | boolean | null;

export interface VariantRunResult {
    variantId: string;
    variantName: string;
    isPublic: boolean;
    status: VariantRunStatus;
    columns: string[];
    rows: RunCell[][];
    rowCount: number;
    truncated: boolean;
    runtimeMs: number;
    message: string | null;
}

export interface StudioRun {
    id: string;
    dialect: Dialect;
    status: StudioRunStatus;
    sql: string;
    createdAt: string;
    completedAt: string | null;
    rejection: { verdict: string; message: string } | null;
    systemError: string | null;
    variants: VariantRunResult[];
}

export type AnswerCheckStatus = "PENDING" | "PASSED" | "FAILED";

export type MismatchReason = "NONE" | "COLUMN_COUNT" | "COLUMN_NAMES" | "ROWS" | "ORDER";

export interface DialectCheckResult {
    dialect: Dialect;
    verdict: Verdict | null;
    mismatch: MismatchReason;
    message: string | null;
    runtimeMs: number;
    rowCount: number;
}

export interface AnswerCheckTestcase {
    testcaseId: string;
    position: number;
    isPublic: boolean;
    isPassed: boolean;
    systemError: string | null;
    dialects: DialectCheckResult[];
}

export interface AnswerCheck {
    id: string;
    status: AnswerCheckStatus;
    testcaseCount: number;
    checkedCount: number;
    isCurrent: boolean;
    createdAt: string;
    completedAt: string | null;
    testcases: AnswerCheckTestcase[];
}

export interface CreateStudioRunRequest {
    dialect: Dialect;
    sql: string;
}

export const EDITABLE_STATUSES: readonly ProblemStatus[] = ["DRAFT", "CHANGES_REQUESTED"];

export type ReviewDecision = "APPROVED" | "CHANGES_REQUESTED" | "REJECTED";

export interface ProblemReview {
    decision: ReviewDecision;
    reason: string | null;
    reviewedAt: string;
}
