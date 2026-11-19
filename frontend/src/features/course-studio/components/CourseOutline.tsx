"use client";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";

import { useCourseOutline } from "../hooks/useCourseOutline";
import { AddChapterForm } from "./AddChapterForm";
import { ChapterCard } from "./ChapterCard";

interface CourseOutlineProps {
    courseId: string;
    isLocked: boolean;
}

export function CourseOutline({ courseId, isLocked }: CourseOutlineProps) {
    const { data: chapters, error, isPending, refetch } = useCourseOutline(courseId);

    return (
        <section aria-labelledby="course-outline-title" className="flex flex-col gap-3">
            <h2 id="course-outline-title" className="text-lg font-bold">
                Đề cương
            </h2>
            {isPending ? (
                <div role="status" aria-label="Đang tải đề cương" className="flex flex-col gap-3">
                    <Skeleton className="h-32" />
                    <Skeleton className="h-32" />
                </div>
            ) : null}
            {error && !chapters ? (
                <ErrorState
                    title="Không tải được đề cương"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {chapters && chapters.length === 0 ? (
                <EmptyState title="Khoá chưa có chương nào." />
            ) : null}
            {chapters?.map((chapter) => (
                <ChapterCard
                    key={chapter.id}
                    courseId={courseId}
                    chapter={chapter}
                    isLocked={isLocked}
                />
            ))}
            {isLocked || !chapters ? null : <AddChapterForm courseId={courseId} />}
        </section>
    );
}
