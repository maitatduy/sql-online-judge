import type { Difficulty } from "@/lib/difficulty";

export type ProblemProgress = "SOLVED" | "ATTEMPTED" | "NOT_STARTED";

export interface Topic {
    slug: string;
    name: string;
}

export interface ProblemSummary {
    id: string;
    number: number;
    slug: string;
    title: string;
    difficulty: Difficulty;
    topics: Topic[];
    acceptanceRate: number;
    solvedCount: number;
    progress: ProblemProgress | null;
}

export interface TopicsResponse {
    items: Topic[];
}
