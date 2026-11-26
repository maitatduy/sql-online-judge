"use client";

import { GraduationCap } from "lucide-react";

import { Breadcrumb } from "@/components/shared/Breadcrumb";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { Pagination } from "@/components/shared/Pagination";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";

import { coursesHref, useCourseFilters } from "../hooks/useCourseFilters";
import { useCourses } from "../hooks/useCourses";
import { CourseCard } from "./CourseCard";
import { DifficultyFilter } from "./DifficultyFilter";

const SKELETON_CARDS = 3;

export function CourseCatalogue() {
    const { filters, changeDifficulty } = useCourseFilters();
    const { data: courses, error, isPending, refetch } = useCourses(filters);

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-6xl flex-col gap-5 px-4 pt-7 pb-16 md:px-6"
        >
            <div className="flex flex-col gap-2">
                <Breadcrumb
                    items={[{ label: "Trang chủ", href: ROUTES.home }, { label: "Khoá học" }]}
                />
                <div className="flex flex-wrap items-center justify-between gap-3">
                    <h1 className="text-[22px] font-extrabold tracking-tight">Khoá học</h1>
                    <DifficultyFilter value={filters.difficulty} onChange={changeDifficulty} />
                </div>
            </div>
            {isPending ? (
                <div
                    role="status"
                    aria-label="Đang tải khoá học"
                    className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
                >
                    {Array.from({ length: SKELETON_CARDS }, (_, index) => (
                        <Skeleton key={index} className="h-40 rounded-lg" />
                    ))}
                </div>
            ) : null}
            {!courses && error ? (
                <ErrorState
                    title="Chưa tải được khoá học"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {courses && courses.totalItems === 0 ? (
                <EmptyState icon={<GraduationCap />} title="Chưa có khoá học nào" />
            ) : null}
            {courses && courses.totalItems > 0 ? (
                <>
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {courses.items.map((course) => (
                            <CourseCard key={course.id} course={course} />
                        ))}
                    </div>
                    <Pagination
                        result={courses}
                        unit="khoá học"
                        hrefOf={(page) => coursesHref({ ...filters, page })}
                    />
                </>
            ) : null}
        </main>
    );
}
