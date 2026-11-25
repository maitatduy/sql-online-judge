import { apiClient } from "@/lib/api-client";

import type { LandingContest, LandingPage, LandingRatedUser, TotalCount } from "./types";

const UPCOMING_CONTEST_COUNT = 3;
const TOP_RATED_COUNT = 5;

type CountParams = Record<string, string>;

async function totalOf(path: string, params: CountParams, signal?: AbortSignal) {
    const page = await apiClient.get<TotalCount>(path, {
        params: { ...params, page: 1, size: 1 },
        signal,
    });
    return page.totalItems;
}

export const landingApi = {
    total: totalOf,
    upcomingContests: (signal?: AbortSignal) =>
        apiClient.get<LandingPage<LandingContest>>("/api/contests", {
            params: { phase: "UPCOMING", page: 1, size: UPCOMING_CONTEST_COUNT },
            signal,
        }),
    topRated: (signal?: AbortSignal) =>
        apiClient.get<LandingPage<LandingRatedUser>>("/api/leaderboard", {
            params: { page: 1, size: TOP_RATED_COUNT },
            signal,
        }),
};
