"use client";

import { useState } from "react";

import Link from "next/link";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError, isUnauthenticatedError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { useProblemSubmissions } from "../hooks/useProblemSubmissions";

import { HistoryPager } from "./HistoryPager";
import { SubmissionHistoryRow } from "./SubmissionHistoryRow";

const SKELETON_ROW_COUNT = 4;

interface SubmissionHistoryProps {
    problemId: string;
    loginPath: string;
    now?: Date;
}

export function SubmissionHistory({ problemId, loginPath, now }: SubmissionHistoryProps) {
    const [mountedAt] = useState(() => new Date());
    const [page, setPage] = useState(1);
    const { data, error, isPending, isPlaceholderData, refetch } = useProblemSubmissions(
        problemId,
        page,
    );

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
                <Link href={ROUTES.loginWithNext(loginPath)} className="font-semibold text-brand">
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

    if (!data || data.items.length === 0) {
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
                        <th scope="col">Mã đã nộp</th>
                    </tr>
                </thead>
                <tbody>
                    {data.items.map((submission) => (
                        <SubmissionHistoryRow
                            key={submission.id}
                            submission={submission}
                            now={now ?? mountedAt}
                        />
                    ))}
                </tbody>
            </table>
            <HistoryPager result={data} isLoading={isPlaceholderData} onPageChange={setPage} />
        </div>
    );
}
