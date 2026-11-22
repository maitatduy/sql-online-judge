import { apiClient } from "@/lib/api-client";
import type { ContestPhase } from "@/lib/contest";

import type { ContestDetail, ContestPage, ContestProblem, ContestRegistration } from "./types";

const CONTEST_PAGE_SIZE = 20;

function registrationPath(slug: string): string {
    return `/api/contests/${encodeURIComponent(slug)}/registration`;
}

export const contestApi = {
    list: (phase: ContestPhase, signal?: AbortSignal) =>
        apiClient.get<ContestPage>("/api/contests", {
            params: { phase, page: 1, size: CONTEST_PAGE_SIZE },
            signal,
        }),
    get: (slug: string, signal?: AbortSignal) =>
        apiClient.get<ContestDetail>(`/api/contests/${encodeURIComponent(slug)}`, { signal }),
    problems: (slug: string, signal?: AbortSignal) =>
        apiClient.get<ContestProblem[]>(`/api/contests/${encodeURIComponent(slug)}/problems`, {
            signal,
        }),
    registration: (slug: string, signal?: AbortSignal) =>
        apiClient.get<ContestRegistration>(registrationPath(slug), { signal }),
    register: (slug: string) => apiClient.post<ContestRegistration>(registrationPath(slug)),
    unregister: (slug: string) => apiClient.delete<void>(registrationPath(slug)),
};
