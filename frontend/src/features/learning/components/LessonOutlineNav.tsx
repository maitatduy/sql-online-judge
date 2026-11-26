"use client";

import { BookOpen, ChevronDown, Circle, CircleCheck } from "lucide-react";
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

export function LessonOutlineNav({
    course,
    currentSlug,
    completedLessonKeys,
}: LessonOutlineNavProps) {
    const [toggled, setToggled] = useState<Record<string, boolean>>({});

    function isOpen(chapter: ChapterOutline): boolean {
        return toggled[chapter.id] ?? chapter.lessons.some((lesson) => lesson.slug === currentSlug);
    }

    return (
        <nav aria-label="Đề cương" className="flex flex-col">
            <Link
                href={ROUTES.course(course.slug)}
                className="border-b px-5 py-4 text-[15px] leading-snug font-bold hover:text-brand"
            >
                {course.title}
            </Link>
            <ol className="flex flex-col">
                {course.chapters.map((chapter, chapterIndex) => (
                    <OutlineChapter
                        key={chapter.id}
                        courseSlug={course.slug}
                        chapter={chapter}
                        chapterNumber={chapterIndex + 1}
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
    currentSlug: string;
    completedLessonKeys?: ReadonlySet<string>;
    isOpen: boolean;
    onToggle: () => void;
}

function OutlineChapter({
    courseSlug,
    chapter,
    chapterNumber,
    currentSlug,
    completedLessonKeys,
    isOpen,
    onToggle,
}: OutlineChapterProps) {
    const lessonsId = useId();

    return (
        <li className="border-b">
            <button
                type="button"
                aria-expanded={isOpen}
                aria-controls={lessonsId}
                onClick={onToggle}
                className="flex w-full items-start justify-between gap-3 px-5 py-4 text-left transition-colors hover:bg-surface-2"
            >
                <span className="flex min-w-0 flex-col gap-0.5">
                    <span className="text-[13px] font-semibold text-muted-foreground">
                        {`Chương ${chapterNumber}`}
                        <span className="sr-only">:</span>
                    </span>{" "}
                    <span className="text-[15px] leading-snug font-bold">{chapter.title}</span>
                </span>
                <ChevronDown
                    aria-hidden="true"
                    className={cn(
                        "mt-1 size-5 shrink-0 text-muted-foreground transition-transform",
                        isOpen && "rotate-180",
                    )}
                />
            </button>
            {isOpen ? (
                <ol id={lessonsId} className="flex flex-col pb-2">
                    {chapter.lessons.map((lesson) => {
                        const isCurrent = lesson.slug === currentSlug;
                        const isCompleted = completedLessonKeys?.has(lesson.lessonKey) === true;
                        return (
                            <li key={lesson.lessonKey}>
                                <Link
                                    href={ROUTES.lesson(courseSlug, lesson.slug)}
                                    aria-current={isCurrent ? "page" : undefined}
                                    className={cn(
                                        "flex items-start gap-3 border-l-[3px] border-transparent py-2.5 pr-5 pl-[17px] transition-colors hover:bg-surface-2",
                                        isCurrent && "border-brand bg-brand-soft",
                                    )}
                                >
                                    {isCompleted ? (
                                        <>
                                            <CircleCheck
                                                aria-hidden="true"
                                                className="mt-0.5 size-[18px] shrink-0 fill-brand text-card"
                                            />
                                            <span className="sr-only">Đã hoàn thành</span>
                                        </>
                                    ) : (
                                        <Circle
                                            aria-hidden="true"
                                            className="mt-0.5 size-[18px] shrink-0 text-muted-foreground"
                                        />
                                    )}
                                    <span className="flex min-w-0 flex-col gap-0.5">
                                        <span
                                            className={cn(
                                                "text-[14px] leading-snug",
                                                isCurrent && "font-semibold",
                                            )}
                                        >
                                            {lesson.title}
                                        </span>
                                        <span className="inline-flex items-center gap-1 text-[12px] text-muted-foreground">
                                            <BookOpen aria-hidden="true" className="size-3.5" />
                                            Bài đọc
                                        </span>
                                    </span>
                                </Link>
                            </li>
                        );
                    })}
                </ol>
            ) : null}
        </li>
    );
}
