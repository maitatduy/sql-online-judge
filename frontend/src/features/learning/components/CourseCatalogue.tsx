"use client";

import { GraduationCap } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";

import { useCourses } from "../hooks/useCourses";
import { useDifficultyFilter } from "../hooks/useDifficultyFilter";
import { CourseCard } from "./CourseCard";
import { DifficultyFilter } from "./DifficultyFilter";

const SKELETON_CARDS = 3;

export function CourseCatalogue() {
    const { difficulty, changeDifficulty } = useDifficultyFilter();
    const { data: courses, error, isPending, refetch } = useCourses(difficulty);

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-6xl flex-col gap-5 px-4 pt-7 pb-16 md:px-6"
        >
            <div className="flex flex-wrap items-center justify-between gap-3">
                <h1 className="text-[22px] font-extrabold tracking-tight">Khoá học</h1>
                <DifficultyFilter value={difficulty} onChange={changeDifficulty} />
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
            {courses && courses.items.length === 0 ? (
                <EmptyState icon={<GraduationCap />} title="Chưa có khoá học nào" />
            ) : null}
            {courses && courses.items.length > 0 ? (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {courses.items.map((course) => (
                        <CourseCard key={course.id} course={course} />
                    ))}
                </div>
            ) : null}
        </main>
    );
}
