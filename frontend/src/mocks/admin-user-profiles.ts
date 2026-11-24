import type { SolvedSummary } from "@/features/profile/types";

import { findAdminUser, readAdminUserDetail, readAdminUsers } from "./admin-user-store";
import type { MockProfile } from "./profile-fixtures";

const EASY_TOTAL = 120;
const MEDIUM_TOTAL = 150;
const HARD_TOTAL = 60;
const HARD_SHARE = 0.1;
const MEDIUM_SHARE = 0.3;
const RANK_BASE = 3000;

function solvedByDifficultyOf(acceptedCount: number): SolvedSummary["byDifficulty"] {
    const hard = Math.min(HARD_TOTAL, Math.floor(acceptedCount * HARD_SHARE));
    const medium = Math.min(MEDIUM_TOTAL, Math.floor(acceptedCount * MEDIUM_SHARE));
    const easy = Math.min(EASY_TOTAL, acceptedCount - hard - medium);
    return {
        EASY: { solved: easy, total: EASY_TOTAL },
        MEDIUM: { solved: medium, total: MEDIUM_TOTAL },
        HARD: { solved: hard, total: HARD_TOTAL },
    };
}

export function adminUserProfile(username: string): MockProfile | undefined {
    const user = findAdminUser(username) && readAdminUserDetail(username);
    if (!user) {
        return undefined;
    }
    const isRated = user.rating > 0;
    return {
        profile: {
            userId: user.id,
            username: user.username,
            displayName: user.displayName ?? user.username,
            avatarUrl: user.avatarUrl,
            bio: null,
            school: user.school,
            location: null,
            websiteUrl: null,
            joinedAt: user.joinedAt,
        },
        rating: {
            userId: user.id,
            rating: isRated ? user.rating : null,
            rank: isRated ? Math.max(1, RANK_BASE - user.rating) : null,
            contestsPlayed: isRated ? 1 : 0,
        },
        solved: {
            userId: user.id,
            solvedCount: user.acceptedCount,
            byDifficulty: solvedByDifficultyOf(user.acceptedCount),
        },
    };
}

export function adminUserProfileById(userId: string): MockProfile | undefined {
    const user = readAdminUsers(new Date()).find((candidate) => candidate.id === userId);
    return user && adminUserProfile(user.username);
}
