import { BookOpen } from "lucide-react";
import Link from "next/link";

import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import type { EnrolledCourse } from "../../types";
import { CourseProgressBar } from "../CourseProgressBar";
import { DashboardSection } from "./DashboardSection";

interface ContinueLearningSectionProps {
    enrollments: EnrolledCourse[] | undefined;
}

const OTHER_COURSES = 3;

function hrefOf(course: EnrolledCourse): string {
    return course.nextLessonSlug
        ? ROUTES.lesson(course.slug, course.nextLessonSlug)
        : ROUTES.course(course.slug);
}

export function ContinueLearningSection({ enrollments }: ContinueLearningSectionProps) {
    const current =
        enrollments?.find((course) => course.nextLessonSlug !== null) ?? enrollments?.[0];
    const others = (enrollments ?? [])
        .filter((course) => course !== current)
        .slice(0, OTHER_COURSES);

    return (
        <DashboardSection
            title="Học tiếp"
            action={{ label: "Tất cả khoá", href: ROUTES.learn }}
            className="flex-[1_1_420px]"
        >
            {enrollments === undefined ? <Skeleton className="h-28 rounded-lg" /> : null}
            {enrollments?.length === 0 ? (
                <div className="flex flex-col items-start gap-3 rounded-lg bg-surface-2 p-5">
                    <p className="font-semibold">Chưa học khoá nào</p>
                    <Button asChild>
                        <Link href={ROUTES.learn}>Xem khoá học</Link>
                    </Button>
                </div>
            ) : null}
            {current ? (
                <div className="flex flex-wrap items-center gap-4 rounded-lg bg-surface-2 p-5">
                    <span
                        aria-hidden="true"
                        className="flex size-12 shrink-0 items-center justify-center rounded-lg bg-brand/12 text-brand"
                    >
                        <BookOpen className="size-6" />
                    </span>
                    <div className="flex min-w-0 flex-1 flex-col gap-2">
                        <p className="text-sm text-muted-foreground">
                            {`Đã học ${formatNumber(current.completedCount)}/${formatNumber(current.lessonCount)} bài`}
                        </p>
                        <p className="truncate text-lg font-bold">{current.title}</p>
                        <CourseProgressBar
                            completedCount={current.completedCount}
                            lessonCount={current.lessonCount}
                        />
                    </div>
                    <Button asChild>
                        <Link href={hrefOf(current)}>
                            {current.nextLessonSlug ? "Tiếp tục" : "Ôn lại"}
                        </Link>
                    </Button>
                </div>
            ) : null}
            {others.length > 0 ? (
                <ul className="grid gap-3 sm:grid-cols-3">
                    {others.map((course) => (
                        <li key={course.slug}>
                            <Link
                                href={hrefOf(course)}
                                className="flex flex-col gap-2 rounded-lg border p-4 transition-colors hover:border-brand"
                            >
                                <span className="truncate font-semibold">{course.title}</span>
                                <CourseProgressBar
                                    completedCount={course.completedCount}
                                    lessonCount={course.lessonCount}
                                />
                            </Link>
                        </li>
                    ))}
                </ul>
            ) : null}
        </DashboardSection>
    );
}
