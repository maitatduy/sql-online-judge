"use client";

import Link from "next/link";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { useReviewCourse, useReviewOutline } from "../../hooks/useCourseReviews";
import { CourseDecisionForm } from "./CourseDecisionForm";

interface CourseReviewDetailProps {
    courseId: string;
    onReviewed: () => void;
}

export function CourseReviewDetail({ courseId, onReviewed }: CourseReviewDetailProps) {
    const course = useReviewCourse(courseId);
    const outline = useReviewOutline(courseId);
    const error = course.error ?? outline.error;

    return (
        <section
            aria-label="Chi tiết khoá"
            className="flex flex-col gap-4 rounded-lg border bg-card p-5"
        >
            {course.isPending || outline.isPending ? (
                <div role="status" aria-label="Đang tải khoá" className="flex flex-col gap-3">
                    <Skeleton className="h-8 w-2/3" />
                    <Skeleton className="h-40" />
                </div>
            ) : null}
            {error ? (
                <ErrorState
                    title="Chưa tải được khoá"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void Promise.all([course.refetch(), outline.refetch()])}
                    className="border-0"
                />
            ) : null}
            {course.data && outline.data ? (
                <>
                    <div>
                        <div className="flex flex-wrap items-center gap-3">
                            <h2 className="text-xl font-extrabold">{course.data.title}</h2>
                            <DifficultyLabel difficulty={course.data.difficulty} />
                        </div>
                        <p className="mt-1 text-muted-foreground">{course.data.summary}</p>
                        <p className="mt-1 text-sm text-muted-foreground">
                            {formatNumber(course.data.lessonCount)} bài giảng,{" "}
                            {formatNumber(course.data.estimatedMinutes)} phút
                        </p>
                    </div>
                    <ol className="flex flex-col gap-3">
                        {outline.data.map((chapter) => (
                            <li key={chapter.id} className="rounded-md border">
                                <p className="border-b px-4 py-2 font-bold">
                                    {chapter.position}. {chapter.title}
                                </p>
                                <ul className="flex flex-col divide-y">
                                    {chapter.lessons.map((lesson) => (
                                        <li
                                            key={lesson.id}
                                            className="flex items-center gap-3 px-4 py-2"
                                        >
                                            <Link
                                                href={ROUTES.studioLesson(courseId, lesson.id)}
                                                target="_blank"
                                                className="flex-1 hover:text-brand hover:underline"
                                            >
                                                {lesson.title}
                                            </Link>
                                            <span className="text-sm text-muted-foreground">
                                                {formatNumber(lesson.estimatedMinutes)} phút
                                            </span>
                                        </li>
                                    ))}
                                </ul>
                            </li>
                        ))}
                    </ol>
                    <CourseDecisionForm
                        courseId={courseId}
                        title={course.data.title}
                        onReviewed={onReviewed}
                    />
                </>
            ) : null}
        </section>
    );
}
