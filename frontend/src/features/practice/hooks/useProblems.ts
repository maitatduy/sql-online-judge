import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { practiceApi } from "../api";
import type { ProblemFilters } from "../problem-filters";

export function useProblems(filters: ProblemFilters) {
    return useQuery({
        queryKey: ["problems", filters],
        queryFn: ({ signal }) => practiceApi.listProblems(filters, signal),
        placeholderData: keepPreviousData,
    });
}
