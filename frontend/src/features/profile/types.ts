import type { Dialect } from "@/lib/dialect";
import type { Difficulty } from "@/lib/difficulty";
import type { Verdict } from "@/lib/verdict";

export interface ProfileBadge {
    code: string;
    name: string;
    earnedAt: string;
}

export interface SolvedProgress {
    solved: number;
    total: number;
}

export interface ProfileStats {
    solvedCount: number;
    globalRank: number | null;
    currentStreakDays: number;
    longestStreakDays: number;
    solvedByDifficulty: Record<Difficulty, SolvedProgress>;
}

export interface ProfileTabCounts {
    sharedSolutions: number;
    posts: number;
    contests: number;
}

export interface ProfileResponse {
    username: string;
    displayName: string;
    bio?: string;
    school?: string;
    location?: string;
    websiteUrl?: string;
    rating: number;
    rankTitle: string;
    followerCount: number;
    followingCount: number;
    badges: ProfileBadge[];
    stats: ProfileStats;
    tabCounts: ProfileTabCounts;
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

export interface ActivityDay {
    date: string;
    count: number;
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
