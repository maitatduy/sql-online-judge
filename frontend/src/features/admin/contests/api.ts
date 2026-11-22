import { apiClient } from "@/lib/api-client";

import type { ReviewProblemPage } from "../problem-reviews/types";
import type { AdminContest, AdminContestPage, CreateContestRequest } from "./types";

export const ADMIN_CONTESTS_PAGE_SIZE = 100;

export const adminContestApi = {
    list: (signal?: AbortSignal) =>
        apiClient.get<AdminContestPage>("/api/admin/contests", {
            params: { page: 1, size: ADMIN_CONTESTS_PAGE_SIZE },
            signal,
        }),
    get: (contestId: string, signal?: AbortSignal) =>
        apiClient.get<AdminContest>(`/api/admin/contests/${encodeURIComponent(contestId)}`, {
            signal,
        }),
    create: (request: CreateContestRequest) =>
        apiClient.post<AdminContest>("/api/admin/contests", request),
    replaceProblems: (contestId: string, problemIds: string[]) =>
        apiClient.put<AdminContest>(
            `/api/admin/contests/${encodeURIComponent(contestId)}/problems`,
            { problemIds },
        ),
    contestReadyProblems: (signal?: AbortSignal) =>
        apiClient.get<ReviewProblemPage>("/api/admin/problems", {
            params: {
                status: "PUBLISHED",
                visibility: "CONTEST_ONLY",
                page: 1,
                size: ADMIN_CONTESTS_PAGE_SIZE,
            },
            signal,
        }),
};
