import { useSearchParams } from "next/navigation";

import {
    auditHref,
    NO_AUDIT_FILTERS,
    readAuditFilters,
    type AuditFilters,
} from "../audit/search-params";

export type AuditFilterChange = Partial<Omit<AuditFilters, "page">>;

function replaceUrl(filters: AuditFilters): void {
    window.history.replaceState(null, "", auditHref(filters));
}

function changeFilters(change: AuditFilterChange): void {
    const current = readAuditFilters(new URLSearchParams(window.location.search));
    replaceUrl({ ...current, ...change, page: NO_AUDIT_FILTERS.page });
}

function clearFilters(): void {
    replaceUrl(NO_AUDIT_FILTERS);
}

export function useAuditFilters() {
    const filters = readAuditFilters(useSearchParams());

    return { filters, changeFilters, clearFilters };
}
