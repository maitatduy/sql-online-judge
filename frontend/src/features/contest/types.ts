import type { ContestPhase } from "@/lib/contest";
import type { Difficulty } from "@/lib/difficulty";
import type { Page } from "@/lib/pagination";

export interface ContestSummary {
    id: string;
    slug: string;
    title: string;
    startAt: string;
    endAt: string;
    durationMinutes: number;
    isRated: boolean;
    phase: ContestPhase;
}

export type ContestPage = Page<ContestSummary>;

export interface ContestDetail extends ContestSummary {
    description: string;
    freezeMinutes: number;
    penaltyMinutes: number;
    registrantCount: number;
}

export interface ContestRegistration {
    isRegistered: boolean;
    registeredAt: string | null;
}

export interface ContestProblem {
    letter: string;
    title: string;
    difficulty: Difficulty;
}

export interface StandingCell {
    letter: string;
    acceptedMinute: number | null;
    wrongAttempts: number;
    pendingAttempts: number;
}

export interface StandingItem {
    rank: number;
    username: string;
    displayName: string | null;
    solved: number;
    penaltyMinutes: number;
    cells: StandingCell[];
}

export interface StandingsPage extends Page<StandingItem> {
    problems: string[];
    isFrozen: boolean;
}

export interface MyStanding {
    solved: number;
    penaltyMinutes: number;
    cells: StandingCell[];
}
