import { useSearchParams } from "next/navigation";

import {
    bankHref,
    NO_BANK_FILTERS,
    readBankFilters,
    type ProblemBankFilters,
} from "../problem-bank/search-params";

export type BankFilterChange = Partial<Omit<ProblemBankFilters, "page">>;

function currentFilters(): ProblemBankFilters {
    return readBankFilters(new URLSearchParams(window.location.search));
}

function changeFilters(change: BankFilterChange): void {
    window.history.replaceState(
        null,
        "",
        bankHref({ ...currentFilters(), ...change, page: NO_BANK_FILTERS.page }),
    );
}

function clearFilters(): void {
    window.history.replaceState(null, "", bankHref(NO_BANK_FILTERS));
}

export function useProblemBankFilters() {
    const filters = readBankFilters(useSearchParams());

    return { filters, changeFilters, clearFilters };
}
