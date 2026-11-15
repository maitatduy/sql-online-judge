import { ArrowRight } from "lucide-react";
import Link from "next/link";
import { useId } from "react";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/routes";

import type { EnrolledCourse } from "../../types";
import { CourseProgressBar } from "../CourseProgressBar";

interface EnrolledCourseCardProps {
    course: EnrolledCourse;
}

export function EnrolledCourseCard({ course }: EnrolledCourseCardProps) {
    const titleId = useId();
    const actionHref = course.nextLessonSlug
        ? ROUTES.lesson(course.slug, course.nextLessonSlug)
        : ROUTES.course(course.slug);

    return (
        <article
            aria-labelledby={titleId}
            className="flex flex-col gap-3 rounded-lg border bg-card p-5"
        >
            <DifficultyLabel difficulty={course.difficulty} className="text-sm" />
            <h3 id={titleId} className="text-lg leading-snug font-bold">
                {course.title}
            </h3>
            <CourseProgressBar
                completedCount={course.completedCount}
                lessonCount={course.lessonCount}
            />
            <Button asChild className="mt-1 self-start">
                <Link href={actionHref}>
                    {course.nextLessonSlug ? "Học tiếp" : "Ôn lại"}
                    <ArrowRight aria-hidden="true" />
                </Link>
            </Button>
        </article>
    );
}
