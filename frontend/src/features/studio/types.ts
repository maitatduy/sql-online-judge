import type { Difficulty } from "@/lib/difficulty";

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

export type StatusCounts = Record<ProblemStatus, number>;

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
    latestVersionNumber: number;
    hasDraft: boolean;
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

export type StudioStep = "info" | "statement";

export const EDITABLE_STATUSES: readonly ProblemStatus[] = ["DRAFT", "CHANGES_REQUESTED"];
