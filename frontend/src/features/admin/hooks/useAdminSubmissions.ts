import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useSearchParams } from "next/navigation";

import { adminSubmissionApi } from "../submissions/api";
import {
    NO_SUBMISSION_FILTERS,
    readSubmissionFilters,
    submissionsHref,
    type SubmissionFilters,
} from "../submissions/search-params";

export type SubmissionFilterChange = Partial<Omit<SubmissionFilters, "page">>;

function currentFilters(): SubmissionFilters {
    return readSubmissionFilters(new URLSearchParams(window.location.search));
}

function changeFilters(change: SubmissionFilterChange): void {
    window.history.replaceState(
        null,
        "",
        submissionsHref({ ...currentFilters(), ...change, page: NO_SUBMISSION_FILTERS.page }),
    );
}

function clearFilters(): void {
    window.history.replaceState(null, "", submissionsHref(NO_SUBMISSION_FILTERS));
}

export function useSubmissionFilters() {
    const filters = readSubmissionFilters(useSearchParams());

    return { filters, changeFilters, clearFilters };
}

export function useAdminSubmissions(filters: SubmissionFilters) {
    return useQuery({
        queryKey: ["admin", "submissions", filters],
        queryFn: ({ signal }) => adminSubmissionApi.list(filters, signal),
        placeholderData: keepPreviousData,
    });
}

export function useAdminSubmission(submissionId: string) {
    return useQuery({
        queryKey: ["admin", "submission", submissionId],
        queryFn: ({ signal }) => adminSubmissionApi.detail(submissionId, signal),
    });
}
