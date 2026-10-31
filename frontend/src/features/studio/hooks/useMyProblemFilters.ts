import { useSearchParams } from "next/navigation";

import { type MyProblemFilters, myProblemsHref, readMyProblemFilters } from "../my-problem-filters";

function changeFilters(change: Partial<Omit<MyProblemFilters, "page">>): void {
    const current = readMyProblemFilters(new URLSearchParams(window.location.search));
    window.history.replaceState(null, "", myProblemsHref({ ...current, ...change, page: 1 }));
}

export function useMyProblemFilters() {
    const filters = readMyProblemFilters(useSearchParams());

    return { filters, changeFilters };
}
