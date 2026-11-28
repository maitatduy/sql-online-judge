import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { ContestPhase } from "@/lib/contest";

import { contestApi } from "../api";
import { contestRefetchInterval } from "./useRefreshWhenDue";

const CONTESTS_KEY = "contests";
const CONTEST_KEY = "contest";
const REGISTRATION_KEY = "contest-registration";
const PROBLEMS_KEY = "contest-problems";

export const CONTESTS_QUERY_KEY = [CONTESTS_KEY] as const;

export function useContests(phase: ContestPhase) {
    return useQuery({
        queryKey: [CONTESTS_KEY, phase],
        queryFn: ({ signal }) => contestApi.list(phase, signal),
    });
}

export function useContest(slug: string) {
    return useQuery({
        queryKey: [CONTEST_KEY, slug],
        queryFn: ({ signal }) => contestApi.get(slug, signal),
        refetchInterval: (query) =>
            query.state.data ? contestRefetchInterval(query.state.data, new Date()) : false,
    });
}

export function useContestProblems(slug: string, isSignedIn: boolean) {
    return useQuery({
        queryKey: [PROBLEMS_KEY, slug],
        queryFn: ({ signal }) => contestApi.problems(slug, signal),
        enabled: isSignedIn,
        retry: false,
    });
}

export function useRatingChanges(slug: string) {
    return useQuery({
        queryKey: ["contest-rating-changes", slug],
        queryFn: ({ signal }) => contestApi.ratingChanges(slug, signal),
    });
}

export function useContestRegistration(slug: string, isSignedIn: boolean) {
    return useQuery({
        queryKey: [REGISTRATION_KEY, slug],
        queryFn: ({ signal }) => contestApi.registration(slug, signal),
        enabled: isSignedIn,
    });
}

export function useChangeRegistration(slug: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (isRegistering: boolean): Promise<void> => {
            if (isRegistering) {
                await contestApi.register(slug);
                return;
            }
            await contestApi.unregister(slug);
        },
        onSuccess: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: [REGISTRATION_KEY, slug] }),
                queryClient.invalidateQueries({ queryKey: [CONTEST_KEY, slug] }),
                queryClient.invalidateQueries({ queryKey: [PROBLEMS_KEY, slug] }),
            ]),
    });
}
