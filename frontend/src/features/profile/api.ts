import { apiClient } from "@/lib/api-client";

import type {
    ActivityResponse,
    PublicProfile,
    RatingHistoryResponse,
    RatingSummary,
    RecentSubmissionsResponse,
    SolvedSummary,
} from "./types";

const RECENT_SUBMISSIONS_PAGE_SIZE = 5;

function userPath(username: string): string {
    return `/api/users/${encodeURIComponent(username)}`;
}

export const profileApi = {
    getProfile: (username: string, signal?: AbortSignal) =>
        apiClient.get<PublicProfile>(userPath(username), { signal }),
    getRatingSummary: (userId: string, signal?: AbortSignal) =>
        apiClient.get<RatingSummary>(`/api/leaderboard/${encodeURIComponent(userId)}`, { signal }),
    getSolvedSummary: (userId: string, signal?: AbortSignal) =>
        apiClient.get<SolvedSummary>(`/api/solved-summaries/${encodeURIComponent(userId)}`, {
            signal,
        }),
    getRatingHistory: (username: string, signal?: AbortSignal) =>
        apiClient.get<RatingHistoryResponse>(`${userPath(username)}/rating-history`, { signal }),
    getActivity: (username: string, signal?: AbortSignal) =>
        apiClient.get<ActivityResponse>(`${userPath(username)}/activity`, { signal }),
    getRecentSubmissions: (username: string, cursor?: string, signal?: AbortSignal) =>
        apiClient.get<RecentSubmissionsResponse>(`${userPath(username)}/submissions`, {
            params: { cursor, size: RECENT_SUBMISSIONS_PAGE_SIZE },
            signal,
        }),
};
