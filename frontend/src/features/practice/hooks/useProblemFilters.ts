import { useSearchParams } from "next/navigation";

import {
    NO_PROBLEM_FILTERS,
    problemsHref,
    readProblemFilters,
    type ProblemFilters,
} from "../problem-filters";

export type ProblemFilterChange = Partial<Omit<ProblemFilters, "page">>;

function currentFilters(): ProblemFilters {
    return readProblemFilters(new URLSearchParams(window.location.search));
}

function changeFilters(change: ProblemFilterChange): void {
    const nextFilters = { ...currentFilters(), ...change, page: NO_PROBLEM_FILTERS.page };
    window.history.replaceState(null, "", problemsHref(nextFilters));
}

function clearFilters(): void {
    window.history.replaceState(null, "", problemsHref(NO_PROBLEM_FILTERS));
}

export function useProblemFilters() {
    const filters = readProblemFilters(useSearchParams());

    return { filters, changeFilters, clearFilters };
}
