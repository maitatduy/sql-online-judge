import type { ProfileResponse } from "@/features/profile/types";

import { findAdminUser, readAdminUserDetail } from "./admin-user-store";

const EASY_TOTAL = 120;
const MEDIUM_TOTAL = 150;
const HARD_TOTAL = 60;
const HARD_SHARE = 0.1;
const MEDIUM_SHARE = 0.3;
const EXPERT_RATING = 2000;
const SPECIALIST_RATING = 1400;
const RANK_BASE = 3000;

function rankTitleOf(rating: number): string {
    if (rating === 0) {
        return "Chưa xếp hạng";
    }
    if (rating >= EXPERT_RATING) {
        return "Chuyên gia";
    }
    return rating >= SPECIALIST_RATING ? "Chuyên viên" : "Học viên";
}

function solvedByDifficultyOf(
    acceptedCount: number,
): ProfileResponse["stats"]["solvedByDifficulty"] {
    const hard = Math.min(HARD_TOTAL, Math.floor(acceptedCount * HARD_SHARE));
    const medium = Math.min(MEDIUM_TOTAL, Math.floor(acceptedCount * MEDIUM_SHARE));
    const easy = Math.min(EASY_TOTAL, acceptedCount - hard - medium);
    return {
        EASY: { solved: easy, total: EASY_TOTAL },
        MEDIUM: { solved: medium, total: MEDIUM_TOTAL },
        HARD: { solved: hard, total: HARD_TOTAL },
    };
}

export function adminUserProfile(username: string): ProfileResponse | undefined {
    const user = findAdminUser(username) && readAdminUserDetail(username);
    if (!user) {
        return undefined;
    }
    return {
        username: user.username,
        displayName: user.displayName ?? user.username,
        rating: user.rating,
        rankTitle: rankTitleOf(user.rating),
        followerCount: 0,
        followingCount: 0,
        badges: [],
        stats: {
            solvedCount: user.acceptedCount,
            globalRank: user.rating > 0 ? Math.max(1, RANK_BASE - user.rating) : null,
            currentStreakDays: 0,
            longestStreakDays: 0,
            solvedByDifficulty: solvedByDifficultyOf(user.acceptedCount),
        },
        tabCounts: { sharedSolutions: 0, posts: 0, contests: 0 },
    };
}
