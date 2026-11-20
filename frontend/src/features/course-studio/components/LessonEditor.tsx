"use client";

import Link from "next/link";

import { ChevronLeft } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { useStudioCourse } from "../hooks/useStudioCourse";
import { useStudioLesson } from "../hooks/useStudioLesson";
import { LessonCells } from "./LessonCells";
import { LessonForm } from "./LessonForm";
import { LessonQuizEditor } from "./LessonQuizEditor";

const NOT_FOUND_STATUS = 404;

interface LessonEditorProps {
    courseId: string;
    lessonId: string;
}

export function LessonEditor({ courseId, lessonId }: LessonEditorProps) {
    const course = useStudioCourse(courseId);
    const lesson = useStudioLesson(lessonId);
    const error = lesson.error ?? course.error;
    const isNotFound = error instanceof ApiRequestError && error.status === NOT_FOUND_STATUS;
    const isLocked = course.data ? course.data.revisionStatus !== "DRAFT" : true;

    return (
        <div className="mx-auto flex max-w-[960px] flex-col gap-5 px-6 pt-7 pb-16">
            {course.data ? (
                <Link
                    href={ROUTES.studioCourse(courseId)}
                    className="inline-flex w-fit items-center gap-1 text-muted-foreground hover:text-foreground"
                >
                    <ChevronLeft aria-hidden="true" className="size-4" />
                    {course.data.title}
                </Link>
            ) : null}
            {lesson.isPending || course.isPending ? (
                <div role="status" aria-label="Đang tải bài giảng" className="flex flex-col gap-4">
                    <Skeleton className="h-9 w-2/3" />
                    <Skeleton className="h-[560px]" />
                </div>
            ) : null}
            {isNotFound ? <EmptyState title="Không tìm thấy bài giảng" /> : null}
            {error && !isNotFound ? (
                <ErrorState
                    title="Không tải được bài giảng"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void Promise.all([lesson.refetch(), course.refetch()])}
                />
            ) : null}
            {lesson.data && course.data && !error ? (
                <>
                    <h1 className="text-[28px] font-extrabold tracking-[-0.02em]">
                        {lesson.data.title}
                    </h1>
                    {isLocked ? (
                        <p className="text-muted-foreground">
                            Khoá đang chờ duyệt nên chưa sửa được.
                        </p>
                    ) : null}
                    <LessonForm key={lesson.data.id} lesson={lesson.data} isLocked={isLocked} />
                    <LessonCells lessonId={lesson.data.id} isLocked={isLocked} />
                    <LessonQuizEditor lessonId={lesson.data.id} isLocked={isLocked} />
                </>
            ) : null}
        </div>
    );
}
