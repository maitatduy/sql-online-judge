"use client";

import { Flag } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { useNow } from "@/lib/useNow";
import { cn } from "@/lib/utils";

import { useReportCases } from "../../hooks/useReports";
import { REPORT_REASON_LABELS, REPORT_TARGET_LABELS } from "../../reports/labels";
import type { ReportCase, ReportCaseKey, ReportReason, ReportStatus } from "../../reports/types";

const SKELETON_ITEM_COUNT = 4;

interface ReportCaseListProps {
    status: ReportStatus;
    selected: ReportCaseKey | null;
    onSelect: (key: ReportCaseKey) => void;
}

function topReason(reportCase: ReportCase): ReportReason | null {
    const entries = Object.entries(reportCase.reasons) as [ReportReason, number][];
    return entries.sort((a, b) => b[1] - a[1])[0]?.[0] ?? null;
}

export function ReportCaseList({ status, selected, onSelect }: ReportCaseListProps) {
    const now = useNow();
    const { data, error, isPending, refetch } = useReportCases(status);

    return (
        <section
            aria-label="Danh sách báo cáo"
            className="overflow-hidden rounded-lg border bg-card"
        >
            {isPending ? (
                <div
                    role="status"
                    aria-label="Đang tải báo cáo"
                    className="flex flex-col gap-3 p-4"
                >
                    {Array.from({ length: SKELETON_ITEM_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-14" />
                    ))}
                </div>
            ) : null}
            {!data && error ? (
                <ErrorState
                    title="Chưa tải được báo cáo"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="border-0"
                />
            ) : null}
            {data && data.items.length === 0 ? (
                <EmptyState title="Không có báo cáo nào." icon={<Flag />} className="border-0" />
            ) : null}
            {data && data.items.length > 0 ? (
                <ul className="flex flex-col">
                    {data.items.map((reportCase) => {
                        const isSelected =
                            selected?.targetId === reportCase.targetId &&
                            selected.targetType === reportCase.targetType;
                        const reason = topReason(reportCase);
                        return (
                            <li
                                key={`${reportCase.targetType}-${reportCase.targetId}`}
                                className="border-b last:border-b-0"
                            >
                                <button
                                    type="button"
                                    aria-pressed={isSelected}
                                    onClick={() =>
                                        onSelect({
                                            targetType: reportCase.targetType,
                                            targetId: reportCase.targetId,
                                        })
                                    }
                                    className={cn(
                                        "flex w-full flex-col gap-1.5 px-4 py-3 text-left hover:bg-surface-2",
                                        isSelected && "bg-brand-soft",
                                    )}
                                >
                                    <span className="flex items-center justify-between gap-2 text-sm">
                                        <span className="font-semibold text-muted-foreground">
                                            {REPORT_TARGET_LABELS[reportCase.targetType]}
                                            {reason ? ` - ${REPORT_REASON_LABELS[reason]}` : null}
                                        </span>
                                        <span className="rounded-full bg-count-report px-2 py-0.5 text-xs font-bold text-white">
                                            {formatNumber(reportCase.reportCount)}
                                        </span>
                                    </span>
                                    <span className="line-clamp-2 text-[15px]">
                                        {reportCase.preview}
                                    </span>
                                    <span className="text-[13px] text-muted-foreground">
                                        {`${reportCase.authorUsername ?? "Người dùng"} - ${formatRelativeTime(reportCase.latestReportAt, now)}`}
                                    </span>
                                </button>
                            </li>
                        );
                    })}
                </ul>
            ) : null}
        </section>
    );
}
