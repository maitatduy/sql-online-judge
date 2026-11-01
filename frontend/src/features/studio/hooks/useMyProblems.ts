import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { studioApi } from "../api";
import type { MyProblemFilters } from "../my-problem-filters";

export function useMyProblems(filters: MyProblemFilters) {
    return useQuery({
        queryKey: ["studio-problems", "list", filters],
        queryFn: ({ signal }) => studioApi.listMyProblems(filters, signal),
        placeholderData: keepPreviousData,
    });
}
