import { apiClient } from "@/lib/api-client";

import type {
    ReportCaseDetail,
    ReportCaseKey,
    ReportCasePage,
    ReportStatus,
    ResolveReport,
} from "./types";

export const REPORT_PAGE_SIZE = 50;

function casePath({ targetType, targetId }: ReportCaseKey): string {
    return `/api/admin/reports/${targetType}/${encodeURIComponent(targetId)}`;
}

export const reportApi = {
    cases: (status: ReportStatus, signal?: AbortSignal) =>
        apiClient.get<ReportCasePage>("/api/admin/reports", {
            params: { status, page: 1, size: REPORT_PAGE_SIZE },
            signal,
        }),
    detail: (key: ReportCaseKey, signal?: AbortSignal) =>
        apiClient.get<ReportCaseDetail>(casePath(key), { signal }),
    resolve: ({ action, note, ...key }: ResolveReport) =>
        apiClient.post<void>(`${casePath(key)}/resolve`, { action, note }),
};
