import Link from "next/link";

import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import type { CourseDetail } from "../types";

interface LessonOutlineNavProps {
    course: CourseDetail;
    currentSlug: string;
}

export function LessonOutlineNav({ course, currentSlug }: LessonOutlineNavProps) {
    let lessonNumber = 0;

    return (
        <nav aria-label="Đề cương" className="flex flex-col gap-4">
            <Link
                href={ROUTES.course(course.slug)}
                className="text-[13px] font-bold text-muted-foreground hover:text-foreground"
            >
                {course.title}
            </Link>
            {course.chapters.map((chapter) => (
                <div key={chapter.id} className="flex flex-col gap-1">
                    <p className="px-2 text-[12px] font-semibold text-muted-foreground">
                        {chapter.title}
                    </p>
                    <ol className="flex flex-col">
                        {chapter.lessons.map((lesson) => {
                            lessonNumber += 1;
                            const isCurrent = lesson.slug === currentSlug;
                            return (
                                <li key={lesson.lessonKey}>
                                    <Link
                                        href={ROUTES.lesson(course.slug, lesson.slug)}
                                        aria-current={isCurrent ? "page" : undefined}
                                        className={cn(
                                            "flex items-center gap-2.5 rounded-md px-2 py-2 text-[13px] transition-colors hover:bg-surface-2",
                                            isCurrent && "bg-brand-soft font-semibold text-brand",
                                        )}
                                    >
                                        <span className="w-4 shrink-0 text-right text-[12px] text-muted-foreground">
                                            {lessonNumber}
                                        </span>
                                        <span>{lesson.title}</span>
                                    </Link>
                                </li>
                            );
                        })}
                    </ol>
                </div>
            ))}
        </nav>
    );
}
