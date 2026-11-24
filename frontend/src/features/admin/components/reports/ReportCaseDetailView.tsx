"use client";

import Link from "next/link";
import { useState } from "react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Markdown } from "@/components/shared/Markdown";
import { Skeleton } from "@/components/shared/Skeleton";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";
import { useNow } from "@/lib/useNow";

import { useReportCase, useResolveReport } from "../../hooks/useReports";
import { REPORT_REASON_LABELS, REPORT_TARGET_LABELS } from "../../reports/labels";
import type { ReportAction, ReportCaseDetail, ReportCaseKey } from "../../reports/types";
import { BanUserDialog } from "../users/BanUserDialog";

const MAX_NOTE_LENGTH = 1000;

interface ReportCaseDetailViewProps {
    caseKey: ReportCaseKey;
    isOpen: boolean;
    onResolved: () => void;
}

export function ReportCaseDetailView({ caseKey, isOpen, onResolved }: ReportCaseDetailViewProps) {
    const { data, error, isPending, refetch } = useReportCase(caseKey);

    return (
        <section aria-label="Chi tiết báo cáo" className="rounded-lg border bg-card p-5 sm:p-6">
            {isPending ? (
                <div role="status" aria-label="Đang tải chi tiết" className="flex flex-col gap-3">
                    <Skeleton className="h-6 w-1/2" />
                    <Skeleton className="h-32" />
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
            {data ? <CaseBody detail={data} isOpen={isOpen} onResolved={onResolved} /> : null}
        </section>
    );
}

interface CaseBodyProps {
    detail: ReportCaseDetail;
    isOpen: boolean;
    onResolved: () => void;
}

function contentHref(detail: ReportCaseDetail): string | null {
    if (detail.targetType === "USER") {
        return detail.authorUsername ? ROUTES.profile(detail.authorUsername) : null;
    }
    return detail.content.postId ? ROUTES.communityPost(detail.content.postId) : null;
}

function CaseBody({ detail, isOpen, onResolved }: CaseBodyProps) {
    const now = useNow();
    const showToast = useToast();
    const { data: currentUser } = useCurrentUser();
    const resolve = useResolveReport();
    const [note, setNote] = useState("");
    const [isBanning, setIsBanning] = useState(false);
    const author = detail.authorUsername;
    const href = contentHref(detail);

    const decide = (action: ReportAction) =>
        resolve.mutate(
            {
                targetType: detail.targetType,
                targetId: detail.targetId,
                action,
                note: note.trim() || undefined,
            },
            {
                onSuccess: () => {
                    showToast(action === "REMOVE" ? "Đã xử lý vi phạm." : "Đã bỏ qua báo cáo.");
                    onResolved();
                },
                onError: (error) => showToast(error.message, "error"),
            },
        );

    return (
        <div className="flex flex-col gap-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="flex flex-col gap-1">
                    <p className="text-sm font-semibold text-muted-foreground">
                        {REPORT_TARGET_LABELS[detail.targetType]}
                        {detail.content.isDeleted ? " - đã xoá" : null}
                        {!detail.content.isDeleted && detail.content.isHidden ? " - đang ẩn" : null}
                    </p>
                    <h2 className="text-lg font-bold">
                        {detail.content.title ?? author ?? "Người dùng"}
                    </h2>
                    <p className="text-[15px] text-muted-foreground">
                        {`Tác giả: ${author ?? "Người dùng"} - Vi phạm trước: `}
                        <strong className="text-violation-severe">
                            {formatNumber(detail.priorViolationCount)}
                        </strong>
                    </p>
                </div>
                {href ? (
                    <Button asChild variant="outline" size="sm">
                        <Link href={href} target="_blank">
                            Mở nội dung
                        </Link>
                    </Button>
                ) : null}
            </div>
            {detail.content.body ? (
                <div className="max-h-80 overflow-y-auto rounded-md border bg-surface-2 p-4">
                    <Markdown className="text-[15px]">{detail.content.body}</Markdown>
                </div>
            ) : null}
            <div className="flex flex-col gap-2">
                <h3 className="text-base font-bold">
                    {`Báo cáo: ${formatNumber(detail.reports.length)}`}
                </h3>
                <ul className="flex flex-col divide-y rounded-md border">
                    {detail.reports.map((report) => (
                        <li key={report.id} className="flex flex-col gap-1 px-4 py-2.5 text-[15px]">
                            <span>
                                <strong>{REPORT_REASON_LABELS[report.reason]}</strong>
                                <span className="text-muted-foreground">
                                    {` - ${report.reporterUsername ?? "Người dùng"}, ${formatRelativeTime(report.createdAt, now)}`}
                                </span>
                            </span>
                            {report.detail ? (
                                <span className="text-muted-foreground">{report.detail}</span>
                            ) : null}
                        </li>
                    ))}
                </ul>
            </div>
            {isOpen ? (
                <div className="flex flex-col gap-3 border-t pt-4">
                    <label htmlFor="report-note" className="sr-only">
                        Ghi chú xử lý
                    </label>
                    <textarea
                        id="report-note"
                        rows={2}
                        maxLength={MAX_NOTE_LENGTH}
                        placeholder="Ghi chú xử lý (không bắt buộc)"
                        value={note}
                        onChange={(event) => setNote(event.target.value)}
                        className="rounded-md border bg-transparent px-3 py-2 text-[15px]"
                    />
                    <div className="flex flex-wrap justify-end gap-2">
                        {currentUser?.role === "ADMIN" && author ? (
                            <Button
                                type="button"
                                variant="outline"
                                onClick={() => setIsBanning(true)}
                            >
                                Khoá tác giả
                            </Button>
                        ) : null}
                        <Button
                            type="button"
                            variant="outline"
                            disabled={resolve.isPending}
                            onClick={() => decide("DISMISS")}
                        >
                            Bỏ qua
                        </Button>
                        <Button
                            type="button"
                            variant="destructive"
                            disabled={resolve.isPending}
                            onClick={() => decide("REMOVE")}
                        >
                            {detail.targetType === "USER" ? "Xác nhận vi phạm" : "Gỡ nội dung"}
                        </Button>
                    </div>
                </div>
            ) : null}
            {isBanning && author ? (
                <BanUserDialog username={author} onClose={() => setIsBanning(false)} />
            ) : null}
        </div>
    );
}
