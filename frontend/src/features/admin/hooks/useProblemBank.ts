import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { problemBankApi } from "../problem-bank/api";
import type { ProblemBankFilters } from "../problem-bank/search-params";

export function useProblemBank(filters: ProblemBankFilters) {
    return useQuery({
        queryKey: ["admin", "problems", "bank", filters],
        queryFn: ({ signal }) => problemBankApi.list(filters, signal),
        placeholderData: keepPreviousData,
        retry: false,
    });
}

export function useProblemBankSummary() {
    return useQuery({
        queryKey: ["admin", "problems", "summary"],
        queryFn: ({ signal }) => problemBankApi.summary(signal),
        retry: false,
    });
}

export function useBankTags() {
    return useQuery({
        queryKey: ["admin", "problems", "tags"],
        queryFn: ({ signal }) => problemBankApi.tags(signal),
    });
}
