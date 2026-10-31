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
