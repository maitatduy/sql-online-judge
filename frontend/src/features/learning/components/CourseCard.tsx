import Link from "next/link";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { ROUTES } from "@/lib/routes";

import { courseSummaryPartsOf } from "../course-summary";
import type { CourseSummary } from "../types";
import { CourseMeta } from "./CourseMeta";

interface CourseCardProps {
    course: CourseSummary;
}

export function CourseCard({ course }: CourseCardProps) {
    return (
        <article className="relative flex flex-col gap-2.5 rounded-lg border bg-card p-5 transition-colors focus-within:border-brand hover:border-brand">
            <DifficultyLabel difficulty={course.difficulty} className="text-[12px]" />
            <h2 className="text-base leading-snug font-bold">
                <Link
                    href={ROUTES.course(course.slug)}
                    className="outline-none after:absolute after:inset-0 after:rounded-lg"
                >
                    {course.title}
                </Link>
            </h2>
            <p className="line-clamp-2 text-[13px] text-muted-foreground">
                {courseSummaryPartsOf(course.summary).intro}
            </p>
            <CourseMeta
                lessonCount={course.lessonCount}
                estimatedMinutes={course.estimatedMinutes}
                className="mt-auto pt-2"
            />
        </article>
    );
}
