import type { Difficulty } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

export type ContestPhase = "UPCOMING" | "RUNNING" | "FINISHED";

export interface AdminContestSummary {
    id: string;
    slug: string;
    title: string;
    startAt: string;
    endAt: string;
    durationMinutes: number;
    isRated: boolean;
    phase: ContestPhase;
}

export interface AdminContestProblem {
    letter: string;
    problemId: string;
    slug: string;
    title: string;
    difficulty: Difficulty;
}

export interface AdminContest extends AdminContestSummary {
    description: string;
    freezeMinutes: number;
    penaltyMinutes: number;
    problems: AdminContestProblem[];
}

export type AdminContestPage = Page<AdminContestSummary>;

export interface CreateContestRequest {
    slug: string;
    title: string;
    description: string;
    startAt: string;
    durationMinutes: number;
    freezeMinutes: number;
    penaltyMinutes: number;
    isRated: boolean;
}
