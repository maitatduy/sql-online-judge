import Link from "next/link";

import { ROUTES } from "@/lib/routes";

import type { ChapterOutline } from "../types";
import { CompletedMark } from "./CompletedMark";

interface CourseOutlineProps {
    courseSlug: string;
    chapters: ChapterOutline[];
    completedLessonKeys?: ReadonlySet<string>;
}

export function CourseOutline({ courseSlug, chapters, completedLessonKeys }: CourseOutlineProps) {
    let lessonNumber = 0;

    return (
        <ol aria-label="Đề cương" className="flex flex-col gap-4">
            {chapters.map((chapter) => (
                <li key={chapter.id} className="rounded-lg border bg-card">
                    <h2 className="border-b px-5 py-3 text-base font-bold">{chapter.title}</h2>
                    <ol className="divide-y">
                        {chapter.lessons.map((lesson) => {
                            lessonNumber += 1;
                            return (
                                <li key={lesson.lessonKey}>
                                    <Link
                                        href={ROUTES.lesson(courseSlug, lesson.slug)}
                                        className="flex min-h-12 items-center gap-3 px-5 py-2.5 transition-colors hover:bg-surface-2"
                                    >
                                        <span className="inline-flex size-7 shrink-0 items-center justify-center rounded-full bg-surface-2 text-[12px] font-semibold text-muted-foreground">
                                            {lessonNumber}
                                        </span>
                                        <span className="font-medium">{lesson.title}</span>
                                        {completedLessonKeys?.has(lesson.lessonKey) ? (
                                            <CompletedMark />
                                        ) : null}
                                    </Link>
                                </li>
                            );
                        })}
                    </ol>
                </li>
            ))}
        </ol>
    );
}
