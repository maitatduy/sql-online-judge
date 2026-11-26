"use client";

import { Check, ChevronRight } from "lucide-react";
import Link from "next/link";
import { useId, useState } from "react";

import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import type { ChapterOutline, CourseDetail } from "../types";

interface LessonOutlineNavProps {
    course: CourseDetail;
    currentSlug: string;
    completedLessonKeys?: ReadonlySet<string>;
}

function firstNumbersOf(chapters: ChapterOutline[]): number[] {
    let next = 1;
    return chapters.map((chapter) => {
        const first = next;
        next += chapter.lessons.length;
        return first;
    });
}

export function LessonOutlineNav({
    course,
    currentSlug,
    completedLessonKeys,
}: LessonOutlineNavProps) {
    const [toggled, setToggled] = useState<Record<string, boolean>>({});
    const firstNumbers = firstNumbersOf(course.chapters);

    function isOpen(chapter: ChapterOutline): boolean {
        return toggled[chapter.id] ?? chapter.lessons.some((lesson) => lesson.slug === currentSlug);
    }

    return (
        <nav
            aria-label="Đề cương"
            className="flex max-h-[calc(100vh-6rem)] flex-col overflow-hidden rounded-lg border bg-card"
        >
            <Link
                href={ROUTES.course(course.slug)}
                className="border-b px-4 py-3 text-[15px] leading-snug font-bold hover:text-brand"
            >
                {course.title}
            </Link>
            <ol className="flex flex-col gap-1 overflow-y-auto p-2">
                {course.chapters.map((chapter, chapterIndex) => (
                    <OutlineChapter
                        key={chapter.id}
                        courseSlug={course.slug}
                        chapter={chapter}
                        chapterNumber={chapterIndex + 1}
                        firstLessonNumber={firstNumbers[chapterIndex]}
                        currentSlug={currentSlug}
                        completedLessonKeys={completedLessonKeys}
                        isOpen={isOpen(chapter)}
                        onToggle={() =>
                            setToggled((current) => ({
                                ...current,
                                [chapter.id]: !isOpen(chapter),
                            }))
                        }
                    />
                ))}
            </ol>
        </nav>
    );
}

interface OutlineChapterProps {
    courseSlug: string;
    chapter: ChapterOutline;
    chapterNumber: number;
    firstLessonNumber: number;
    currentSlug: string;
    completedLessonKeys?: ReadonlySet<string>;
    isOpen: boolean;
    onToggle: () => void;
}

function OutlineChapter({
    courseSlug,
    chapter,
    chapterNumber,
    firstLessonNumber,
    currentSlug,
    completedLessonKeys,
    isOpen,
    onToggle,
}: OutlineChapterProps) {
    const lessonsId = useId();

    return (
        <li>
            <button
                type="button"
                aria-expanded={isOpen}
                aria-controls={lessonsId}
                onClick={onToggle}
                className="flex w-full items-start gap-1.5 rounded-md px-2 py-2 text-left text-[13px] font-semibold transition-colors hover:bg-surface-2"
            >
                <ChevronRight
                    aria-hidden="true"
                    className={cn(
                        "mt-0.5 size-4 shrink-0 text-muted-foreground transition-transform",
                        isOpen && "rotate-90",
                    )}
                />
                <span>
                    <span className="text-brand">{`Chương ${chapterNumber}:`}</span> {chapter.title}
                </span>
            </button>
            {isOpen ? (
                <ol id={lessonsId} className="ml-4 flex flex-col border-l py-1 pl-2">
                    {chapter.lessons.map((lesson, lessonIndex) => {
                        const isCurrent = lesson.slug === currentSlug;
                        const isCompleted = completedLessonKeys?.has(lesson.lessonKey);
                        return (
                            <li key={lesson.lessonKey}>
                                <Link
                                    href={ROUTES.lesson(courseSlug, lesson.slug)}
                                    aria-current={isCurrent ? "page" : undefined}
                                    className={cn(
                                        "flex items-center gap-2.5 rounded-md px-2 py-1.5 text-[14px] transition-colors hover:bg-surface-2",
                                        isCurrent && "bg-brand-soft font-semibold text-brand",
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
                                                <Check aria-hidden="true" className="size-3" />
                                                <span className="sr-only">Đã hoàn thành</span>
                                            </>
                                        ) : (
                                            firstLessonNumber + lessonIndex
                                        )}
                                    </span>
                                    <span className="min-w-0 flex-1">{lesson.title}</span>
                                </Link>
                            </li>
                        );
                    })}
                </ol>
            ) : null}
        </li>
    );
}
