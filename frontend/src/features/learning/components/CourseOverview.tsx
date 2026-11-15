"use client";

import { ArrowRight, SearchX } from "lucide-react";
import Link from "next/link";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { useCourse } from "../hooks/useCourse";
import { useCourseProgress } from "../hooks/useCourseProgress";
import type { CourseDetail } from "../types";
import { CourseMeta } from "./CourseMeta";
import { CourseOutline } from "./CourseOutline";
import { CourseProgressBar } from "./CourseProgressBar";

interface CourseOverviewProps {
    slug: string;
}

function firstLessonSlug(course: CourseDetail): string | undefined {
    return course.chapters.flatMap((chapter) => chapter.lessons)[0]?.slug;
}

export function CourseOverview({ slug }: CourseOverviewProps) {
    const { data: course, error, isPending, refetch } = useCourse(slug);
    const isNotFound = error instanceof ApiRequestError && error.status === 404;

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-4 pt-7 pb-16 md:px-6"
        >
            {isPending ? (
                <div role="status" aria-label="Đang tải khoá học" className="flex flex-col gap-3">
                    <Skeleton className="h-8 w-2/3" />
                    <Skeleton className="h-4 w-full" />
                    <Skeleton className="h-48 rounded-lg" />
                </div>
            ) : null}
            {isNotFound ? (
                <EmptyState
                    icon={<SearchX />}
                    title="Không tìm thấy khoá học"
                    action={
                        <Button asChild size="sm" variant="outline">
                            <Link href={ROUTES.learn}>Xem khoá học khác</Link>
                        </Button>
                    }
                />
            ) : null}
            {error && !isNotFound ? (
                <ErrorState
                    title="Chưa tải được khoá học"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {course ? <CourseContent course={course} /> : null}
        </main>
    );
}

function CourseContent({ course }: { course: CourseDetail }) {
    const { data: currentUser } = useCurrentUser();
    const { data: progress } = useCourseProgress(course.slug, currentUser !== undefined);
    const startSlug = firstLessonSlug(course);
    const completedKeys = new Set(progress?.completedLessonKeys);
    const isLearning = progress !== undefined && (progress.enrolled || completedKeys.size > 0);
    const action = !isLearning
        ? { label: "Bắt đầu học", slug: startSlug }
        : progress.nextLessonSlug
          ? { label: "Học tiếp", slug: progress.nextLessonSlug }
          : { label: "Ôn lại", slug: startSlug };

    return (
        <>
            <section className="flex flex-col gap-3">
                <DifficultyLabel difficulty={course.difficulty} className="text-[12px]" />
                <h1 className="text-[22px] font-extrabold tracking-tight">{course.title}</h1>
                <p className="text-muted-foreground">{course.summary}</p>
                <CourseMeta
                    chapterCount={course.chapters.length}
                    lessonCount={course.lessonCount}
                    estimatedMinutes={course.estimatedMinutes}
                />
                {isLearning ? (
                    <CourseProgressBar
                        completedCount={completedKeys.size}
                        lessonCount={progress.lessonCount}
                    />
                ) : null}
                {action.slug ? (
                    <Button asChild className="mt-1 self-start">
                        <Link href={ROUTES.lesson(course.slug, action.slug)}>
                            {action.label}
                            <ArrowRight aria-hidden="true" />
                        </Link>
                    </Button>
                ) : null}
            </section>
            <CourseOutline
                courseSlug={course.slug}
                chapters={course.chapters}
                completedLessonKeys={completedKeys}
            />
        </>
    );
}
