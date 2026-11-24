import type {
    PublicProfile,
    RatingPoint,
    RatingSummary,
    SolvedSummary,
} from "@/features/profile/types";

import { DEMO_SESSION_USER } from "./session";

export interface MockProfile {
    profile: PublicProfile;
    rating: RatingSummary;
    solved: SolvedSummary;
}

const MINH_ANH_ID = DEMO_SESSION_USER.id;
const NEWBIE_ID = "0192f3a0-0000-7000-8000-0000000000e1";

export const MINH_ANH_PROFILE: MockProfile = {
    profile: {
        userId: MINH_ANH_ID,
        username: DEMO_SESSION_USER.username,
        displayName: DEMO_SESSION_USER.displayName,
        avatarUrl: null,
        bio: "Sinh viên năm 3 Khoa học dữ liệu. Đang luyện SQL để đi thực tập Data Analyst.",
        school: "ĐH Bách khoa Hà Nội",
        location: "Hà Nội, Việt Nam",
        websiteUrl: "https://github.com/minhanh-data",
        joinedAt: "2026-03-01T02:00:00Z",
    },
    rating: { userId: MINH_ANH_ID, rating: 1487, rank: 2341, contestsPlayed: 8 },
    solved: {
        userId: MINH_ANH_ID,
        solvedCount: 63,
        byDifficulty: {
            EASY: { solved: 42, total: 120 },
            MEDIUM: { solved: 18, total: 150 },
            HARD: { solved: 3, total: 60 },
        },
    },
};

export const NEWBIE_PROFILE: MockProfile = {
    profile: {
        userId: NEWBIE_ID,
        username: "newbie",
        displayName: "Trần Thu Hà",
        avatarUrl: null,
        bio: null,
        school: null,
        location: null,
        websiteUrl: null,
        joinedAt: "2026-11-01T02:00:00Z",
    },
    rating: { userId: NEWBIE_ID, rating: null, rank: null, contestsPlayed: 0 },
    solved: {
        userId: NEWBIE_ID,
        solvedCount: 0,
        byDifficulty: {
            EASY: { solved: 0, total: 120 },
            MEDIUM: { solved: 0, total: 150 },
            HARD: { solved: 0, total: 60 },
        },
    },
};

const MINH_ANH_CONTEST_RESULTS: [rating: number, rank: number, finishedAt: string][] = [
    [1320, 29, "2026-05-03T14:00:00Z"],
    [1352, 41, "2026-05-24T14:00:00Z"],
    [1338, 77, "2026-06-14T14:00:00Z"],
    [1395, 23, "2026-07-05T14:00:00Z"],
    [1428, 31, "2026-07-26T14:00:00Z"],
    [1404, 88, "2026-08-16T14:00:00Z"],
    [1441, 27, "2026-08-30T14:00:00Z"],
    [1487, 36, "2026-09-20T14:00:00Z"],
];
const FIRST_CONTEST_NUMBER = 12;

export const RATING_HISTORY_BY_USERNAME: Record<string, RatingPoint[]> = {
    [MINH_ANH_PROFILE.profile.username]: MINH_ANH_CONTEST_RESULTS.map(
        ([rating, rank, finishedAt], index) => ({
            contestSlug: `weekly-${FIRST_CONTEST_NUMBER + index}`,
            contestTitle: `SQL Judge Weekly #${FIRST_CONTEST_NUMBER + index}`,
            rating,
            rank,
            finishedAt,
        }),
    ),
};

export const BROKEN_PROFILE_USERNAME = "error";
export const MOCK_TRACE_ID = "4bf92f3577b34da6";

export const PROFILES: MockProfile[] = [MINH_ANH_PROFILE, NEWBIE_PROFILE];
