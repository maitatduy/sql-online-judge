import type { ProfileResponse, RatingPoint } from "@/features/profile/types";

import { DEMO_SESSION_USER } from "./session";

export const MINH_ANH_PROFILE: ProfileResponse = {
    username: DEMO_SESSION_USER.username,
    displayName: DEMO_SESSION_USER.displayName,
    bio: "Sinh viên năm 3 Khoa học dữ liệu. Đang luyện SQL để đi thực tập Data Analyst.",
    school: "ĐH Bách khoa Hà Nội",
    location: "Hà Nội, Việt Nam",
    websiteUrl: "https://github.com/minhanh-data",
    rating: 1487,
    rankTitle: "Chuyên viên",
    followerCount: 128,
    followingCount: 41,
    badges: [
        { code: "STREAK_10", name: "Chuỗi 10 ngày", earnedAt: "2026-09-28T02:00:00Z" },
        { code: "JOIN_MASTER", name: "Bậc thầy JOIN", earnedAt: "2026-09-12T09:30:00Z" },
        { code: "CONTEST_TOP_20", name: "Top 20% contest", earnedAt: "2026-08-30T14:00:00Z" },
        { code: "SOLVED_50", name: "50 bài AC", earnedAt: "2026-08-17T11:15:00Z" },
        { code: "FIRST_CONTEST", name: "Contest đầu tiên", earnedAt: "2026-05-03T14:00:00Z" },
        { code: "SOLVED_10", name: "10 bài AC", earnedAt: "2026-04-11T08:45:00Z" },
        { code: "FIRST_AC", name: "AC đầu tiên", earnedAt: "2026-03-20T13:10:00Z" },
    ],
    stats: {
        solvedCount: 63,
        globalRank: 2341,
        currentStreakDays: DEMO_SESSION_USER.currentStreakDays,
        longestStreakDays: 31,
        solvedByDifficulty: {
            EASY: { solved: 42, total: 120 },
            MEDIUM: { solved: 18, total: 150 },
            HARD: { solved: 3, total: 60 },
        },
    },
    tabCounts: { sharedSolutions: 6, posts: 3, contests: 8 },
};

export const NEWBIE_PROFILE: ProfileResponse = {
    username: "newbie",
    displayName: "Trần Thu Hà",
    rating: 0,
    rankTitle: "Chưa xếp hạng",
    followerCount: 0,
    followingCount: 0,
    badges: [],
    stats: {
        solvedCount: 0,
        globalRank: null,
        currentStreakDays: 0,
        longestStreakDays: 0,
        solvedByDifficulty: {
            EASY: { solved: 0, total: 120 },
            MEDIUM: { solved: 0, total: 150 },
            HARD: { solved: 0, total: 60 },
        },
    },
    tabCounts: { sharedSolutions: 0, posts: 0, contests: 0 },
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
    [MINH_ANH_PROFILE.username]: MINH_ANH_CONTEST_RESULTS.map(
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

export const PROFILES: ProfileResponse[] = [MINH_ANH_PROFILE, NEWBIE_PROFILE];
