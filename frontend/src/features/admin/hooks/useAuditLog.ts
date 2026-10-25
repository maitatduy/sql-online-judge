import { keepPreviousData, useQuery } from "@tanstack/react-query";

import { adminApi } from "../api";
import type { AuditFilters } from "../audit/search-params";

export function useAuditLog(filters: AuditFilters) {
    return useQuery({
        queryKey: ["admin", "audit-log", filters],
        queryFn: ({ signal }) => adminApi.getAuditLog(filters, signal),
        placeholderData: keepPreviousData,
        retry: false,
    });
}
