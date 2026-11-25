"use client";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { ApiRequestError } from "@/lib/api-client";
import { DIALECT_LABELS } from "@/lib/dialect";
import { formatNumber } from "@/lib/format";

import { useAdminSubmission } from "../../hooks/useAdminSubmissions";
import type { AdminSubmissionDetail } from "../../submissions/types";
import { SubmissionResult } from "./SubmissionResult";
import { problemLabel } from "../../submissions/labels";

interface SubmissionDetailDialogProps {
    submissionId: string;
    onClose: () => void;
}

function factsOf(submission: AdminSubmissionDetail): [string, string][] {
    return [
        ["Người nộp", submission.username ?? "Tài khoản đã xoá"],
        ["Dialect", DIALECT_LABELS[submission.dialect]],
        ["Nguồn", submission.contestId ? "Contest" : "Luyện tập"],
        [
            "Testcase",
            `${formatNumber(submission.judgedTestcases)}/${formatNumber(submission.totalTestcases)}`,
        ],
        [
            "Testcase sai đầu tiên",
            submission.firstFailedIndex === null ? "-" : `#${submission.firstFailedIndex}`,
        ],
        [
            "Thời gian chạy",
            submission.runtimeMs === null ? "-" : `${formatNumber(submission.runtimeMs)} ms`,
        ],
        ["Lần chấm", formatNumber(submission.attempts)],
        ["Lúc nộp", new Date(submission.createdAt).toLocaleString("vi-VN")],
    ];
}

export function SubmissionDetailDialog({ submissionId, onClose }: SubmissionDetailDialogProps) {
    const { data: submission, error, isPending, refetch } = useAdminSubmission(submissionId);

    return (
        <Dialog open onOpenChange={(isOpen) => (isOpen ? undefined : onClose())}>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
                <div className="flex flex-wrap items-center gap-2">
                    <DialogTitle>
                        {submission ? problemLabel(submission) : "Chi tiết bài nộp"}
                    </DialogTitle>
                    {submission ? <SubmissionResult submission={submission} /> : null}
                </div>
                <DialogDescription className="sr-only">
                    Code và kết quả chấm của bài nộp
                </DialogDescription>
                {isPending ? (
                    <div role="status" aria-label="Đang tải bài nộp" className="grid gap-3">
                        <Skeleton className="h-24" />
                        <Skeleton className="h-40" />
                    </div>
                ) : null}
                {error && !submission ? (
                    <ErrorState
                        title="Chưa tải được bài nộp"
                        message={error.message}
                        traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                        onRetry={() => void refetch()}
                    />
                ) : null}
                {submission ? (
                    <div className="grid gap-4">
                        <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm sm:grid-cols-4">
                            {factsOf(submission).map(([label, value]) => (
                                <div key={label}>
                                    <dt className="text-muted-foreground">{label}</dt>
                                    <dd className="font-semibold break-words">{value}</dd>
                                </div>
                            ))}
                        </dl>
                        {submission.errorMessage ? (
                            <pre className="rounded-md border border-verdict-re/30 bg-verdict-re/12 p-3 font-mono text-[13px] whitespace-pre-wrap">
                                {submission.errorMessage}
                            </pre>
                        ) : null}
                        <pre
                            aria-label="Code"
                            className="max-h-80 overflow-auto rounded-md border bg-surface p-3 font-mono text-[13px] whitespace-pre"
                        >
                            {submission.code}
                        </pre>
                    </div>
                ) : null}
            </DialogContent>
        </Dialog>
    );
}
