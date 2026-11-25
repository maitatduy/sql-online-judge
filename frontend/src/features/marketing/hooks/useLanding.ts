import { useQueries, useQuery } from "@tanstack/react-query";

import { landingApi } from "../api";
import type { LandingStat } from "../types";

const LANDING_STALE_TIME_MS = 5 * 60_000;

interface StatSource {
    label: string;
    path: string;
    params: Record<string, string>;
}

const STAT_SOURCES: StatSource[] = [
    { label: "bài tập có máy chấm", path: "/api/problems", params: {} },
    { label: "khoá học", path: "/api/courses", params: {} },
    { label: "contest đã tổ chức", path: "/api/contests", params: { phase: "FINISHED" } },
    { label: "người có rating", path: "/api/leaderboard", params: {} },
];

export function useLandingStats() {
    return useQueries({
        queries: STAT_SOURCES.map((source) => ({
            queryKey: ["landing", "total", source.path, source.params],
            queryFn: ({ signal }: { signal: AbortSignal }) =>
                landingApi.total(source.path, source.params, signal),
            staleTime: LANDING_STALE_TIME_MS,
        })),
        combine: (results) => ({
            stats: results.every((result) => result.data !== undefined)
                ? STAT_SOURCES.map<LandingStat>((source, index) => ({
                      label: source.label,
                      value: results[index].data ?? 0,
                  }))
                : undefined,
            isPending: results.some((result) => result.isPending),
            error: results.find((result) => result.error)?.error ?? null,
            refetch: () => results.forEach((result) => void result.refetch()),
        }),
    });
}

export function useTopicCounts(tags: readonly string[]) {
    return useQueries({
        queries: tags.map((tag) => ({
            queryKey: ["landing", "total", "/api/problems", { tag }],
            queryFn: ({ signal }: { signal: AbortSignal }) =>
                landingApi.total("/api/problems", { tag }, signal),
            staleTime: LANDING_STALE_TIME_MS,
        })),
        combine: (results) => results.map((result) => result.data),
    });
}

export function useUpcomingContests() {
    return useQuery({
        queryKey: ["landing", "upcoming-contests"],
        queryFn: ({ signal }) => landingApi.upcomingContests(signal),
        staleTime: LANDING_STALE_TIME_MS,
    });
}

export function useTopRated() {
    return useQuery({
        queryKey: ["landing", "top-rated"],
        queryFn: ({ signal }) => landingApi.topRated(signal),
        staleTime: LANDING_STALE_TIME_MS,
    });
}
