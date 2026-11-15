"use client";

import { SearchX } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Markdown } from "@/components/shared/Markdown";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ApiRequestError, isUnauthenticatedError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { useCourse } from "../hooks/useCourse";
import { useCourseProgress } from "../hooks/useCourseProgress";
import { useLesson } from "../hooks/useLesson";
import { neighboursOf } from "../lesson-navigation";
import { LessonCompletion } from "./LessonCompletion";
import { LessonOutlineNav } from "./LessonOutlineNav";
import { LessonPager } from "./LessonPager";
import { LessonSqlCell } from "./LessonSqlCell";

interface LessonReaderProps {
    courseSlug: string;
    lessonSlug: string;
}

const LESSON_BODY_CLASSES =
    "[&_h2]:mt-2 [&_h2]:text-lg [&_h2]:font-bold [&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:border [&_pre]:bg-surface-2 [&_pre]:p-4 [&_pre_code]:bg-transparent [&_pre_code]:p-0";

export function LessonReader({ courseSlug, lessonSlug }: LessonReaderProps) {
    const pathname = usePathname();
    const router = useRouter();
    const { data: lesson, error, isPending, refetch } = useLesson(courseSlug, lessonSlug);
    const { data: course } = useCourse(courseSlug);
    const { data: progress } = useCourseProgress(courseSlug, lesson !== undefined);
    const completedKeys = new Set(progress?.completedLessonKeys);
    const isUnauthenticated = isUnauthenticatedError(error);
    const isNotFound = error instanceof ApiRequestError && error.status === 404;

    useEffect(() => {
        if (isUnauthenticated) {
            router.replace(ROUTES.loginWithNext(pathname));
        }
    }, [isUnauthenticated, pathname, router]);

    return (
        <div className="mx-auto grid w-full max-w-6xl gap-8 px-4 pt-7 pb-16 md:px-6 lg:grid-cols-[240px_minmax(0,1fr)]">
            <aside className="hidden lg:block">
                {course ? (
                    <div className="sticky top-20">
                        <LessonOutlineNav
                            course={course}
                            currentSlug={lessonSlug}
                            completedLessonKeys={completedKeys}
                        />
                    </div>
                ) : null}
            </aside>
            <main id={MAIN_CONTENT_ID} className="flex max-w-3xl min-w-0 flex-col gap-6">
                {isPending || isUnauthenticated ? (
                    <div
                        role="status"
                        aria-label="Đang tải bài giảng"
                        className="flex flex-col gap-3"
                    >
                        <Skeleton className="h-4 w-1/4" />
                        <Skeleton className="h-8 w-2/3" />
                        <Skeleton className="h-40 rounded-lg" />
                    </div>
                ) : null}
                {isNotFound ? (
                    <EmptyState
                        icon={<SearchX />}
                        title="Không tìm thấy bài giảng"
                        action={
                            <Button asChild size="sm" variant="outline">
                                <Link href={ROUTES.course(courseSlug)}>Về khoá học</Link>
                            </Button>
                        }
                    />
                ) : null}
                {error && !isNotFound && !isUnauthenticated ? (
                    <ErrorState
                        title="Chưa tải được bài giảng"
                        message={error.message}
                        traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                        onRetry={() => void refetch()}
                    />
                ) : null}
                {lesson ? (
                    <>
                        <header className="flex flex-col gap-1.5">
                            <p className="text-[13px] font-semibold text-brand">
                                {lesson.chapterTitle}
                            </p>
                            <h1 className="text-[26px] leading-tight font-extrabold tracking-tight">
                                {lesson.title}
                            </h1>
                        </header>
                        <Markdown className={LESSON_BODY_CLASSES}>{lesson.body}</Markdown>
                        {lesson.cells.map((cell) => (
                            <LessonSqlCell key={cell.cellKey} cell={cell} />
                        ))}
                        {progress ? (
                            <LessonCompletion
                                courseSlug={courseSlug}
                                lessonSlug={lessonSlug}
                                isCompleted={completedKeys.has(lesson.lessonKey)}
                            />
                        ) : null}
                        {course ? (
                            <LessonPager
                                courseSlug={courseSlug}
                                neighbours={neighboursOf(course.chapters, lessonSlug)}
                            />
                        ) : null}
                    </>
                ) : null}
            </main>
        </div>
    );
}
