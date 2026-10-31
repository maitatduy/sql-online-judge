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

export const EDITABLE_STATUSES: readonly ProblemStatus[] = ["DRAFT", "CHANGES_REQUESTED"];
