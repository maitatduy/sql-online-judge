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

function submissionsPath(userId: string): string {
    return `/api/submissions/users/${encodeURIComponent(userId)}`;
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
    getRatingHistory: (userId: string, signal?: AbortSignal) =>
        apiClient.get<RatingHistoryResponse>(
            `/api/leaderboard/${encodeURIComponent(userId)}/history`,
            { signal },
        ),
    getActivity: (userId: string, signal?: AbortSignal) =>
        apiClient.get<ActivityResponse>(`${submissionsPath(userId)}/activity`, { signal }),
    getRecentSubmissions: (userId: string, cursor?: string, signal?: AbortSignal) =>
        apiClient.get<RecentSubmissionsResponse>(submissionsPath(userId), {
            params: { cursor, size: RECENT_SUBMISSIONS_PAGE_SIZE },
            signal,
        }),
};
