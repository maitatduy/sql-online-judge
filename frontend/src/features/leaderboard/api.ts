import { apiClient } from "@/lib/api-client";
import type { Page } from "@/lib/pagination";

export const LEADERBOARD_PAGE_SIZE = 20;

export interface LeaderboardEntry {
    rank: number;
    userId: string;
    username: string | null;
    displayName: string | null;
    rating: number;
    contestsPlayed: number;
}

export const leaderboardApi = {
    page: (page: number, signal?: AbortSignal) =>
        apiClient.get<Page<LeaderboardEntry>>("/api/leaderboard", {
            params: { page, size: LEADERBOARD_PAGE_SIZE },
            signal,
        }),
};
