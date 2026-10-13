"use client";

import { Code } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { DIALECT_LABELS } from "@/lib/dialect";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { useIsProfileOwner } from "../hooks/useIsProfileOwner";
import { useRecentSubmissions } from "../hooks/useRecentSubmissions";

const SKELETON_ROW_COUNT = 5;
const CELL_CLASS_NAME = "border-b px-3 py-2.5 align-middle";

interface RecentSubmissionsTableProps {
    username: string;
    now?: Date;
}

export function RecentSubmissionsTable({ username, now }: RecentSubmissionsTableProps) {
    const [mountedAt] = useState(() => new Date());
    const isOwner = useIsProfileOwner(username);
    const {
        submissions,
        data,
        isFetchNextPageError,
        error,
        isPending,
        hasNextPage,
        isFetchingNextPage,
        fetchNextPage,
        refetch,
    } = useRecentSubmissions(username);

    if (isPending) {
        return (
            <div
                role="status"
                aria-busy="true"
                aria-label="Đang tải bài nộp"
                className="flex flex-col gap-4 p-4"
            >
                {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                    <Skeleton key={index} className="h-4" />
                ))}
            </div>
        );
    }

    if (error && !data) {
        return (
            <ErrorState
                title="Không tải được bài nộp"
                message={error.message}
                traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                onRetry={() => void refetch()}
                className="border-0"
            />
        );
    }

    if (submissions.length === 0) {
        return (
            <EmptyState
                icon={<Code />}
                title="Chưa có bài nộp nào"
                description="Bài nộp gần đây sẽ hiện ở đây."
                action={
                    isOwner ? (
                        <Button asChild size="sm">
                            <Link href={ROUTES.problems}>Giải bài đầu tiên</Link>
                        </Button>
                    ) : undefined
                }
                className="m-4"
            />
        );
    }

    return (
        <>
            <div className="overflow-x-auto">
                <table aria-label="Bài nộp gần đây" className="w-full border-collapse text-[13px]">
                    <thead className="sr-only">
                        <tr>
                            <th scope="col">Kết quả</th>
                            <th scope="col">Bài</th>
                            <th scope="col">Dialect</th>
                            <th scope="col">Thời gian nộp</th>
                        </tr>
                    </thead>
                    <tbody>
                        {submissions.map((submission) => (
                            <tr key={submission.id}>
                                <td className={`${CELL_CLASS_NAME} w-18`}>
                                    <VerdictBadge verdict={submission.verdict} />
                                </td>
                                <td className={`${CELL_CLASS_NAME} font-semibold`}>
                                    #{formatNumber(submission.problemNumber)} ·{" "}
                                    {submission.problemTitle}
                                </td>
                                <td className={`${CELL_CLASS_NAME} text-muted-foreground`}>
                                    {DIALECT_LABELS[submission.dialect]}
                                </td>
                                <td
                                    className={`${CELL_CLASS_NAME} text-right whitespace-nowrap text-muted-foreground`}
                                >
                                    <time dateTime={submission.submittedAt}>
                                        {formatRelativeTime(
                                            submission.submittedAt,
                                            now ?? mountedAt,
                                        )}
                                    </time>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            {hasNextPage ? (
                <div className="flex flex-col items-center gap-2 p-3">
                    {isFetchNextPageError ? (
                        <p role="alert" className="text-[13px] text-destructive">
                            Không tải thêm được bài nộp, hãy thử lại
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
        </>
    );
}
