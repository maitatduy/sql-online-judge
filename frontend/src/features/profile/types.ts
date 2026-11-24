import type { ActivityDay } from "@/lib/activity-calendar";
import type { Dialect } from "@/lib/dialect";
import type { Difficulty } from "@/lib/difficulty";
import type { Verdict } from "@/lib/verdict";

export interface SolvedProgress {
    solved: number;
    total: number;
}

export interface PublicProfile {
    userId: string;
    username: string;
    displayName: string | null;
    avatarUrl: string | null;
    bio: string | null;
    school: string | null;
    location: string | null;
    websiteUrl: string | null;
    joinedAt: string;
}

export interface RatingSummary {
    userId: string;
    rating: number | null;
    rank: number | null;
    contestsPlayed: number;
}

export interface SolvedSummary {
    userId: string;
    solvedCount: number;
    byDifficulty: Record<Difficulty, SolvedProgress>;
}

export interface RatingPoint {
    contestSlug: string;
    contestTitle: string;
    rating: number;
    rank: number;
    finishedAt: string;
}

export interface RatingHistoryResponse {
    items: RatingPoint[];
}

export interface ActivityResponse {
    totalSubmissions: number;
    days: ActivityDay[];
}

export interface RecentSubmission {
    id: string;
    verdict: Verdict;
    problemNumber: number;
    problemSlug: string;
    problemTitle: string;
    dialect: Dialect;
    submittedAt: string;
}

export interface RecentSubmissionsResponse {
    items: RecentSubmission[];
    nextCursor: string | null;
}
export type { ActivityDay };
