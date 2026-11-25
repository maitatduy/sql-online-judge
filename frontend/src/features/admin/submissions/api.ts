import { apiClient } from "@/lib/api-client";

import { submissionsQueryOf, type SubmissionFilters } from "./search-params";
import type { AdminSubmissionDetail, AdminSubmissionPage } from "./types";

export const SUBMISSIONS_PAGE_SIZE = 20;

export const adminSubmissionApi = {
    list: (filters: SubmissionFilters, signal?: AbortSignal) =>
        apiClient.get<AdminSubmissionPage>("/api/admin/submissions", {
            params: { ...submissionsQueryOf(filters), size: SUBMISSIONS_PAGE_SIZE },
            signal,
        }),
    detail: (submissionId: string, signal?: AbortSignal) =>
        apiClient.get<AdminSubmissionDetail>(
            `/api/admin/submissions/${encodeURIComponent(submissionId)}`,
            { signal },
        ),
};
