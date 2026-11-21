"use client";

import Link from "next/link";

import { ChevronLeft } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { useStudioCourse } from "../hooks/useStudioCourse";
import { CourseInfoForm } from "./CourseInfoForm";
import { CourseOutline } from "./CourseOutline";
import { CoursePublishedPanel } from "./CoursePublishedPanel";
import { CourseSubmitPanel } from "./CourseSubmitPanel";
import { RevisionStatusBadge } from "./RevisionStatusBadge";

const NOT_FOUND_STATUS = 404;

interface CourseStudioProps {
    courseId: string;
}

export function CourseStudio({ courseId }: CourseStudioProps) {
    const { data: course, error, isPending, refetch } = useStudioCourse(courseId);
    const isNotFound = error instanceof ApiRequestError && error.status === NOT_FOUND_STATUS;

    return (
        <div className="mx-auto flex max-w-[880px] flex-col gap-5 px-6 pt-7 pb-16">
            <Link
                href={ROUTES.studioCourses}
                className="inline-flex w-fit items-center gap-1 text-muted-foreground hover:text-foreground"
            >
                <ChevronLeft aria-hidden="true" className="size-4" />
                Khoá học của tôi
            </Link>
            {isPending ? (
                <div role="status" aria-label="Đang tải khoá học" className="flex flex-col gap-4">
                    <Skeleton className="h-9 w-2/3" />
                    <Skeleton className="h-80" />
                </div>
            ) : null}
            {isNotFound ? <EmptyState title="Không tìm thấy khoá học" /> : null}
            {error && !isNotFound && !course ? (
                <ErrorState
                    title="Không tải được khoá học"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {course ? (
                <>
                    <div>
                        <div className="flex flex-wrap items-center gap-3">
                            <h1 className="text-[28px] font-extrabold tracking-[-0.02em]">
                                {course.title}
                            </h1>
                            <RevisionStatusBadge status={course.revisionStatus} />
                        </div>
                        <p className="mt-1 text-muted-foreground">
                            {formatNumber(course.lessonCount)} bài giảng,{" "}
                            {formatNumber(course.estimatedMinutes)} phút
                        </p>
                    </div>
                    <CoursePublishedPanel course={course} />
                    <CourseSubmitPanel course={course} />
                    <CourseInfoForm key={course.revisionId} course={course} />
                    <CourseOutline
                        courseId={course.id}
                        isLocked={course.revisionStatus !== "DRAFT"}
                    />
                </>
            ) : null}
        </div>
    );
}
