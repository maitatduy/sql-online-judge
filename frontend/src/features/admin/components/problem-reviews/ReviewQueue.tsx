"use client";

import { useState } from "react";

import { ClipboardCheck } from "lucide-react";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatRelativeTime } from "@/lib/format";
import { cn } from "@/lib/utils";

import { useReviewQueue } from "../../hooks/useProblemReviews";
import type { ReviewProblemStatus } from "../../problem-reviews/types";

const SKELETON_ITEM_COUNT = 4;

interface ReviewQueueProps {
    status: ReviewProblemStatus;
    selectedId: string | null;
    onSelect: (problemId: string) => void;
}

export function ReviewQueue({ status, selectedId, onSelect }: ReviewQueueProps) {
    const { data, error, isPending, refetch } = useReviewQueue(status);
    const [now] = useState(() => new Date());

    return (
        <section aria-label="Hàng đợi duyệt" className="overflow-hidden rounded-lg border bg-card">
            {isPending ? (
                <div
                    role="status"
                    aria-label="Đang tải hàng đợi"
                    className="flex flex-col gap-3 p-4"
                >
                    {Array.from({ length: SKELETON_ITEM_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-12" />
                    ))}
                </div>
            ) : null}
            {error ? (
                <ErrorState
                    title="Chưa tải được hàng đợi"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="border-0"
                />
            ) : null}
            {data && data.items.length === 0 ? (
                <EmptyState
                    title="Không có bài nào ở trạng thái này."
                    icon={<ClipboardCheck />}
                    className="border-0"
                />
            ) : null}
            {data && data.items.length > 0 ? (
                <ul className="flex flex-col">
                    {data.items.map((problem) => (
                        <li key={problem.id} className="border-b last:border-b-0">
                            <button
                                type="button"
                                aria-pressed={problem.id === selectedId}
                                onClick={() => onSelect(problem.id)}
                                className={cn(
                                    "flex w-full flex-col gap-1 px-4 py-3 text-left hover:bg-surface-2",
                                    problem.id === selectedId && "bg-brand-soft",
                                )}
                            >
                                <span className="flex items-start justify-between gap-2">
                                    <span className="text-sm font-semibold">{problem.title}</span>
                                    <DifficultyLabel difficulty={problem.difficulty} />
                                </span>
                                <span className="text-xs text-muted-foreground">
                                    {problem.number === null ? "" : `Bài số ${problem.number}, `}
                                    cập nhật {formatRelativeTime(problem.updatedAt, now)}
                                </span>
                            </button>
                        </li>
                    ))}
                </ul>
            ) : null}
        </section>
    );
}
