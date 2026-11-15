import { useQuery } from "@tanstack/react-query";

import { learningApi } from "../api";

const RECENT_SUBMISSIONS = 5;
const SUGGESTED_PROBLEMS = 4;

export function useSubmissionStats() {
    return useQuery({
        queryKey: ["submission-stats"],
        queryFn: ({ signal }) => learningApi.getSubmissionStats(signal),
    });
}

export function useSubmissionActivity(from: string) {
    return useQuery({
        queryKey: ["submission-activity", from],
        queryFn: ({ signal }) => learningApi.getSubmissionActivity(from, signal),
    });
}

export function useRecentSubmissions() {
    return useQuery({
        queryKey: ["recent-submissions"],
        queryFn: ({ signal }) => learningApi.listRecentSubmissions(RECENT_SUBMISSIONS, signal),
    });
}

export function useSuggestedProblems() {
    return useQuery({
        queryKey: ["suggested-problems"],
        queryFn: ({ signal }) => learningApi.listSuggestedProblems(SUGGESTED_PROBLEMS, signal),
    });
}
