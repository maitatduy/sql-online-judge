import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { reportApi } from "../reports/api";
import type { ReportCaseKey, ReportStatus } from "../reports/types";

export function useReportCases(status: ReportStatus) {
    return useQuery({
        queryKey: ["admin", "reports", status],
        queryFn: ({ signal }) => reportApi.cases(status, signal),
        retry: false,
    });
}

export function useReportCase(key: ReportCaseKey) {
    return useQuery({
        queryKey: ["admin", "report", key.targetType, key.targetId],
        queryFn: ({ signal }) => reportApi.detail(key, signal),
        retry: false,
    });
}

export function useResolveReport() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: reportApi.resolve,
        onSuccess: () =>
            Promise.all([
                queryClient.invalidateQueries({ queryKey: ["admin", "reports"] }),
                queryClient.invalidateQueries({ queryKey: ["admin", "report"] }),
            ]),
    });
}
