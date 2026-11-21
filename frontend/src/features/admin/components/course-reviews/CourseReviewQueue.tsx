"use client";

import { useState } from "react";

import { ClipboardCheck } from "lucide-react";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { cn } from "@/lib/utils";

import { useCourseReviewQueue } from "../../hooks/useCourseReviews";

const SKELETON_ITEM_COUNT = 3;

interface CourseReviewQueueProps {
    selectedId: string | null;
    onSelect: (courseId: string) => void;
}

export function CourseReviewQueue({ selectedId, onSelect }: CourseReviewQueueProps) {
    const { data, error, isPending, refetch } = useCourseReviewQueue();
    const [now] = useState(() => new Date());

    return (
        <section aria-label="Khoá chờ duyệt" className="overflow-hidden rounded-lg border bg-card">
            {isPending ? (
                <div
                    role="status"
                    aria-label="Đang tải hàng chờ"
                    className="flex flex-col gap-3 p-4"
                >
                    {Array.from({ length: SKELETON_ITEM_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-14" />
                    ))}
                </div>
            ) : null}
            {error ? (
                <ErrorState
                    title="Chưa tải được hàng chờ"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="border-0"
                />
            ) : null}
            {data && data.items.length === 0 ? (
                <EmptyState
                    title="Không có khoá nào chờ duyệt."
                    icon={<ClipboardCheck />}
                    className="border-0"
                />
            ) : null}
            {data && data.items.length > 0 ? (
                <ul className="flex flex-col">
                    {data.items.map((course) => (
                        <li key={course.courseId} className="border-b last:border-b-0">
                            <button
                                type="button"
                                aria-pressed={course.courseId === selectedId}
                                onClick={() => onSelect(course.courseId)}
                                className={cn(
                                    "flex w-full flex-col gap-1 px-4 py-3 text-left hover:bg-surface-2",
                                    course.courseId === selectedId && "bg-brand-soft",
                                )}
                            >
                                <span className="flex items-start justify-between gap-2">
                                    <span className="font-semibold">{course.title}</span>
                                    <DifficultyLabel difficulty={course.difficulty} />
                                </span>
                                <span className="text-sm text-muted-foreground">
                                    {formatNumber(course.lessonCount)} bài giảng, gửi{" "}
                                    {formatRelativeTime(course.submittedAt, now)}
                                </span>
                            </button>
                        </li>
                    ))}
                </ul>
            ) : null}
        </section>
    );
}
