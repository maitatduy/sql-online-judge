import { apiClient } from "@/lib/api-client";

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
};
