import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { ContestPhase } from "@/lib/contest";

import { contestApi } from "../api";

const CONTESTS_KEY = "contests";
const CONTEST_KEY = "contest";
const REGISTRATION_KEY = "contest-registration";

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
            ]),
    });
}
