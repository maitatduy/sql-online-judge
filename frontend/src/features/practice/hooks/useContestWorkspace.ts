import { useQuery } from "@tanstack/react-query";

import { practiceApi } from "../api";

export function useContestStatement(slug: string, letter: string) {
    return useQuery({
        queryKey: ["contest-statement", slug, letter],
        queryFn: ({ signal }) => practiceApi.getContestStatement(slug, letter, signal),
        retry: false,
    });
}

export function useContestProblemLinks(slug: string) {
    return useQuery({
        queryKey: ["contest-problems", slug],
        queryFn: ({ signal }) => practiceApi.listContestProblems(slug, signal),
    });
}

export function useContestTiming(slug: string) {
    return useQuery({
        queryKey: ["contest", slug],
        queryFn: ({ signal }) => practiceApi.getContestTiming(slug, signal),
    });
}

const MY_STANDING_REFRESH_MS = 15_000;

export function useMyContestStanding(slug: string) {
    return useQuery({
        queryKey: ["contest-my-standing", slug],
        queryFn: ({ signal }) => practiceApi.getMyContestStanding(slug, signal),
        refetchInterval: MY_STANDING_REFRESH_MS,
        retry: false,
    });
}
