import { Check } from "lucide-react";
import Link from "next/link";

import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import type { CourseDetail } from "../types";

interface LessonOutlineNavProps {
    course: CourseDetail;
    currentSlug: string;
    completedLessonKeys?: ReadonlySet<string>;
}

export function LessonOutlineNav({
    course,
    currentSlug,
    completedLessonKeys,
}: LessonOutlineNavProps) {
    const lessonCount = course.chapters.reduce((sum, chapter) => sum + chapter.lessons.length, 0);
    const completedCount = completedLessonKeys?.size ?? 0;
    const percent = lessonCount ? (100 * completedCount) / lessonCount : 0;
    let lessonNumber = 0;

    return (
        <nav
            aria-label="Đề cương"
            className="flex max-h-[calc(100vh-6rem)] flex-col overflow-hidden rounded-lg border bg-card"
        >
            <div className="flex flex-col gap-2 border-b px-4 py-3">
                <Link
                    href={ROUTES.course(course.slug)}
                    className="leading-snug font-bold hover:text-brand"
                >
                    {course.title}
                </Link>
                <div className="flex items-center gap-2 text-[12px] text-muted-foreground">
                    <span
                        aria-hidden="true"
                        className="h-1.5 flex-1 overflow-hidden rounded-full bg-surface-2"
                    >
                        <span className="block h-full bg-brand" style={{ width: `${percent}%` }} />
                    </span>
                    <span>{`${formatNumber(completedCount)}/${formatNumber(lessonCount)} bài`}</span>
                </div>
            </div>
            <div className="flex flex-col gap-3 overflow-y-auto px-2 py-3">
                {course.chapters.map((chapter, chapterIndex) => (
                    <div key={chapter.id} className="flex flex-col gap-0.5">
                        <p className="px-2 pb-1 text-[12px] font-semibold text-muted-foreground">
                            <span className="text-brand">{`Chương ${chapterIndex + 1}: `}</span>
                            {chapter.title}
                        </p>
                        <ol className="flex flex-col">
                            {chapter.lessons.map((lesson) => {
                                lessonNumber += 1;
                                const isCurrent = lesson.slug === currentSlug;
                                const isCompleted = completedLessonKeys?.has(lesson.lessonKey);
                                return (
                                    <li key={lesson.lessonKey}>
                                        <Link
                                            href={ROUTES.lesson(course.slug, lesson.slug)}
                                            aria-current={isCurrent ? "page" : undefined}
                                            className={cn(
                                                "flex items-center gap-2.5 rounded-md px-2 py-1.5 text-[13px] transition-colors hover:bg-surface-2",
                                                isCurrent &&
                                                    "bg-brand-soft font-semibold text-brand",
                                            )}
                                        >
                                            <span
                                                className={cn(
                                                    "inline-flex size-5 shrink-0 items-center justify-center rounded-full text-[11px] font-semibold",
                                                    isCompleted
                                                        ? "bg-brand text-primary-foreground"
                                                        : "bg-surface-2 text-muted-foreground",
                                                )}
                                            >
                                                {isCompleted ? (
                                                    <>
                                                        <Check
                                                            aria-hidden="true"
                                                            className="size-3"
                                                        />
                                                        <span className="sr-only">
                                                            Đã hoàn thành
                                                        </span>
                                                    </>
                                                ) : (
                                                    lessonNumber
                                                )}
                                            </span>
                                            <span className="min-w-0 flex-1">{lesson.title}</span>
                                        </Link>
                                    </li>
                                );
                            })}
                        </ol>
                    </div>
                ))}
            </div>
        </nav>
    );
}
