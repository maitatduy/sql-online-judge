import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { adminContestApi } from "../contests/api";
import type { CreateContestRequest } from "../contests/types";

const ADMIN_CONTESTS_KEY = "admin-contests";
const ADMIN_CONTEST_KEY = "admin-contest";
const CONTEST_READY_PROBLEMS_KEY = "contest-ready-problems";

export function useAdminContests() {
    return useQuery({
        queryKey: [ADMIN_CONTESTS_KEY],
        queryFn: ({ signal }) => adminContestApi.list(signal),
    });
}

export function useAdminContest(contestId: string) {
    return useQuery({
        queryKey: [ADMIN_CONTEST_KEY, contestId],
        queryFn: ({ signal }) => adminContestApi.get(contestId, signal),
    });
}

export function useCreateContest() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (request: CreateContestRequest) => adminContestApi.create(request),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: [ADMIN_CONTESTS_KEY] }),
    });
}

export function useContestReadyProblems() {
    return useQuery({
        queryKey: [CONTEST_READY_PROBLEMS_KEY],
        queryFn: ({ signal }) => adminContestApi.contestReadyProblems(signal),
    });
}

export function useReplaceContestProblems(contestId: string) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (problemIds: string[]) =>
            adminContestApi.replaceProblems(contestId, problemIds),
        onSuccess: (contest) => queryClient.setQueryData([ADMIN_CONTEST_KEY, contestId], contest),
    });
}
