import { apiClient } from "@/lib/api-client";

import type { QuickEditValues } from "./schemas";
import type { ProblemBankFilters } from "./search-params";
import type {
    AdminTag,
    BankProblemPage,
    BulkUpdateRequest,
    BulkUpdateResult,
    ProblemBankSummary,
} from "./types";

export const BANK_PAGE_SIZE = 20;
const TAGS_PATH = "/api/admin/tags";

export const problemBankApi = {
    list: (filters: ProblemBankFilters, signal?: AbortSignal) =>
        apiClient.get<BankProblemPage>("/api/admin/problems", {
            params: {
                status: "PUBLISHED",
                sort: "number",
                q: filters.q || undefined,
                visibility: filters.visibility ?? undefined,
                difficulty: filters.difficulty ?? undefined,
                tag: filters.tag ?? undefined,
                page: filters.page,
                size: BANK_PAGE_SIZE,
            },
            signal,
        }),
    summary: (signal?: AbortSignal) =>
        apiClient.get<ProblemBankSummary>("/api/admin/problems/summary", { signal }),
    tags: (signal?: AbortSignal) => apiClient.get<AdminTag[]>(TAGS_PATH, { signal }),
    bulkUpdate: (request: BulkUpdateRequest) =>
        apiClient.post<BulkUpdateResult>("/api/admin/problems/bulk-update", request),
    quickEdit: ({ problemId, values }: { problemId: string; values: QuickEditValues }) =>
        apiClient.patch<void>(problemPath(problemId), values),
    hide: (problemId: string) => apiClient.post<void>(`${problemPath(problemId)}/hide`),
    restore: (problemId: string) => apiClient.post<void>(`${problemPath(problemId)}/restore`),
    remove: (problemId: string) => apiClient.delete<void>(problemPath(problemId)),
    transferOwnership: ({ problemId, username }: { problemId: string; username: string }) =>
        apiClient.post<void>(`${problemPath(problemId)}/transfer-ownership`, { username }),
};

function problemPath(problemId: string): string {
    return `/api/admin/problems/${encodeURIComponent(problemId)}`;
}
