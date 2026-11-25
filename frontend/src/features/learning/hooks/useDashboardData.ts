import { useQuery } from "@tanstack/react-query";

import { submissionActivityQuery } from "@/lib/useLearningStreak";

import { learningApi } from "../api";

const RECENT_SUBMISSIONS = 5;
const DAILY_CHALLENGES = 4;
const UPCOMING_CONTESTS = 3;

export function useSubmissionStats() {
    return useQuery({
        queryKey: ["submission-stats"],
        queryFn: ({ signal }) => learningApi.getSubmissionStats(signal),
    });
}

export function useSubmissionActivity(from: string) {
    return useQuery(submissionActivityQuery(from));
}

export function useRecentSubmissions() {
    return useQuery({
        queryKey: ["recent-submissions"],
        queryFn: ({ signal }) => learningApi.listRecentSubmissions(RECENT_SUBMISSIONS, signal),
    });
}

export function useDailyChallenges() {
    return useQuery({
        queryKey: ["daily-challenges"],
        queryFn: ({ signal }) => learningApi.listDailyChallenges(DAILY_CHALLENGES, signal),
    });
}

export function useTopicProgress(userId: string | undefined) {
    return useQuery({
        queryKey: ["topic-progress", userId],
        queryFn: ({ signal }) => learningApi.getTopicProgress(userId ?? "", signal),
        enabled: userId !== undefined,
    });
}

export function useRatingSummary(userId: string | undefined) {
    return useQuery({
        queryKey: ["rating-summary", userId],
        queryFn: ({ signal }) => learningApi.getRatingSummary(userId ?? "", signal),
        enabled: userId !== undefined,
    });
}

export function useUpcomingContests() {
    return useQuery({
        queryKey: ["upcoming-contests", UPCOMING_CONTESTS],
        queryFn: ({ signal }) => learningApi.listUpcomingContests(UPCOMING_CONTESTS, signal),
    });
}
