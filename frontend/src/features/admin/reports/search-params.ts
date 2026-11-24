import { ADMIN_ROUTES } from "@/lib/admin-navigation";

import { REPORT_STATUSES } from "./labels";
import type { ReportCaseKey, ReportStatus, ReportTarget } from "./types";

const TARGETS: readonly ReportTarget[] = ["POST", "COMMENT", "USER"];

export interface ReportSelection {
    status: ReportStatus;
    selected: ReportCaseKey | null;
}

export function readReportSelection(params: URLSearchParams): ReportSelection {
    const status = REPORT_STATUSES.find((candidate) => candidate === params.get("status"));
    const targetType = TARGETS.find((candidate) => candidate === params.get("type"));
    const targetId = params.get("target");
    return {
        status: status ?? "OPEN",
        selected: targetType && targetId ? { targetType, targetId } : null,
    };
}

export function reportsHref({ status, selected }: ReportSelection): string {
    const params = new URLSearchParams();
    if (status !== "OPEN") {
        params.set("status", status);
    }
    if (selected) {
        params.set("type", selected.targetType);
        params.set("target", selected.targetId);
    }
    const query = params.toString();
    return `${ADMIN_ROUTES.reports}${query ? `?${query}` : ""}`;
}
