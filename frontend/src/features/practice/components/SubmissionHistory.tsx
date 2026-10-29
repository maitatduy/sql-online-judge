"use client";

import { useState } from "react";

import Link from "next/link";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { Button } from "@/components/ui/button";
import { ApiRequestError, isUnauthenticatedError } from "@/lib/api-client";
import { DIALECT_LABELS } from "@/lib/dialect";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import { useProblemSubmissions } from "../hooks/useProblemSubmissions";
import type { ProblemDetail } from "../types";

const SKELETON_ROW_COUNT = 4;
const CELL_CLASS = "border-b px-3 py-2.5 align-middle";

interface SubmissionHistoryProps {
    problem: ProblemDetail;
    now?: Date;
}

export function SubmissionHistory({ problem, now }: SubmissionHistoryProps) {
    const [mountedAt] = useState(() => new Date());
    const {
        submissions,
        data,
        error,
        isPending,
        isFetchNextPageError,
        hasNextPage,
        isFetchingNextPage,
        fetchNextPage,
        refetch,
    } = useProblemSubmissions(problem.id);

    if (isPending) {
        return (
            <div
                role="status"
                aria-label="Đang tải lịch sử nộp"
                className="flex flex-col gap-4 p-6"
            >
                {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                    <Skeleton key={index} className="h-4" />
                ))}
            </div>
        );
    }

    if (isUnauthenticatedError(error)) {
        return (
            <p className="px-6 py-5 text-[13px]">
                Đăng nhập để xem lịch sử nộp.{" "}
                <Link
                    href={ROUTES.loginWithNext(ROUTES.problem(problem.slug))}
                    className="font-semibold text-brand"
                >
                    Đăng nhập
                </Link>
            </p>
        );
    }

    if (error && !data) {
        return (
            <ErrorState
                title="Không tải được lịch sử nộp"
                message={error.message}
                traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                onRetry={() => void refetch()}
                className="m-6"
            />
        );
    }

    if (submissions.length === 0) {
        return <EmptyState title="Bạn chưa nộp bài này lần nào." className="m-6" />;
    }

    return (
        <div className="px-6 py-5">
            <table aria-label="Lịch sử nộp" className="w-full border-collapse text-[13px]">
                <thead className="sr-only">
                    <tr>
                        <th scope="col">Kết quả</th>
                        <th scope="col">Dialect</th>
                        <th scope="col">Thời gian chạy</th>
                        <th scope="col">Thời gian nộp</th>
                    </tr>
                </thead>
                <tbody>
                    {submissions.map((submission) => (
                        <tr key={submission.id}>
                            <td className={cn(CELL_CLASS, "w-24")}>
                                <VerdictBadge verdict={submission.verdict ?? "PENDING"} />
                            </td>
                            <td className={cn(CELL_CLASS, "text-muted-foreground")}>
                                {DIALECT_LABELS[submission.dialect]}
                            </td>
                            <td className={cn(CELL_CLASS, "font-mono")}>
                                {submission.runtimeMs === null
                                    ? "-"
                                    : `${formatNumber(submission.runtimeMs)} ms`}
                            </td>
                            <td
                                className={cn(
                                    CELL_CLASS,
                                    "text-right whitespace-nowrap text-muted-foreground",
                                )}
                            >
                                <time dateTime={submission.submittedAt}>
                                    {formatRelativeTime(submission.submittedAt, now ?? mountedAt)}
                                </time>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            {hasNextPage ? (
                <div className="flex flex-col items-center gap-2 pt-3">
                    {isFetchNextPageError ? (
                        <p role="alert" className="text-[13px] text-destructive">
                            Không tải thêm được lịch sử nộp, hãy thử lại
                        </p>
                    ) : null}
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={isFetchingNextPage}
                        onClick={() => void fetchNextPage()}
                    >
                        Xem thêm
                    </Button>
                </div>
            ) : null}
        </div>
    );
}
