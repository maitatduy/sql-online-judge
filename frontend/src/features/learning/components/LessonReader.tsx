"use client";

import { SearchX } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";

import { Breadcrumb } from "@/components/shared/Breadcrumb";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { LessonMarkdown } from "@/components/shared/LessonMarkdown";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ApiRequestError, isUnauthenticatedError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { useCourse } from "../hooks/useCourse";
import { useCourseProgress } from "../hooks/useCourseProgress";
import { useLesson } from "../hooks/useLesson";
import { neighboursOf, positionOf } from "../lesson-navigation";
import { LessonCompletion } from "./LessonCompletion";
import { LessonOutlineNav } from "./LessonOutlineNav";
import { LessonPager } from "./LessonPager";
import { LessonQuiz } from "./LessonQuiz";
import { LessonSqlCell } from "./LessonSqlCell";

interface LessonReaderProps {
    courseSlug: string;
    lessonSlug: string;
}

export function LessonReader({ courseSlug, lessonSlug }: LessonReaderProps) {
    const pathname = usePathname();
    const router = useRouter();
    const { data: lesson, error, isPending, refetch } = useLesson(courseSlug, lessonSlug);
    const { data: course } = useCourse(courseSlug);
    const { data: progress } = useCourseProgress(courseSlug, lesson !== undefined);
    const completedKeys = new Set(progress?.completedLessonKeys);
    const isUnauthenticated = isUnauthenticatedError(error);
    const isNotFound = error instanceof ApiRequestError && error.status === 404;
    const isCompleted = lesson !== undefined && completedKeys.has(lesson.lessonKey);
    const position = course ? positionOf(course.chapters, lessonSlug) : undefined;

    useEffect(() => {
        if (isUnauthenticated) {
            router.replace(ROUTES.loginWithNext(pathname));
        }
    }, [isUnauthenticated, pathname, router]);

    return (
        <div className="mx-auto flex w-full max-w-7xl flex-col gap-5 px-4 pt-7 pb-16 md:px-6">
            <Breadcrumb
                items={[
                    { label: "Trang chủ", href: ROUTES.home },
                    { label: "Khoá học", href: ROUTES.learn },
                    {
                        label: lesson?.courseTitle ?? course?.title ?? "Khoá học",
                        href: ROUTES.course(courseSlug),
                    },
                    ...(lesson ? [{ label: lesson.title }] : []),
                ]}
            />
            <div className="grid gap-6 lg:grid-cols-[280px_minmax(0,1fr)] lg:items-start">
                <aside className="hidden lg:sticky lg:top-20 lg:block">
                    {course ? (
                        <LessonOutlineNav
                            course={course}
                            currentSlug={lessonSlug}
                            completedLessonKeys={completedKeys}
                        />
                    ) : null}
                </aside>
                <main
                    id={MAIN_CONTENT_ID}
                    className="flex min-w-0 flex-col gap-6 rounded-lg border bg-card p-5 md:p-8"
                >
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
                            <header className="flex flex-col gap-2 border-b pb-5">
                                <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[13px] text-muted-foreground">
                                    {position ? (
                                        <span className="rounded-full bg-brand-soft px-2.5 py-0.5 font-semibold text-brand">
                                            {`Chương ${formatNumber(position.chapterNumber)}`}
                                        </span>
                                    ) : null}
                                    <span>{lesson.chapterTitle}</span>
                                    {position ? (
                                        <span>{`Bài ${formatNumber(position.lessonNumber)}/${formatNumber(position.lessonCount)}`}</span>
                                    ) : null}
                                </p>
                                <h1 className="text-[26px] leading-tight font-extrabold tracking-tight">
                                    {lesson.title}
                                </h1>
                            </header>
                            <LessonMarkdown>{lesson.body}</LessonMarkdown>
                            {lesson.cells.map((cell) => (
                                <LessonSqlCell key={cell.cellKey} cell={cell} />
                            ))}
                            {progress && lesson.questions.length > 0 ? (
                                <LessonQuiz
                                    courseSlug={courseSlug}
                                    lessonSlug={lessonSlug}
                                    lessonKey={lesson.lessonKey}
                                    questions={lesson.questions}
                                />
                            ) : null}
                            {progress && (lesson.questions.length === 0 || isCompleted) ? (
                                <LessonCompletion
                                    courseSlug={courseSlug}
                                    lessonSlug={lessonSlug}
                                    isCompleted={isCompleted}
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
        </div>
    );
}
