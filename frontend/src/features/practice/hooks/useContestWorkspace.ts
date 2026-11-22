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
