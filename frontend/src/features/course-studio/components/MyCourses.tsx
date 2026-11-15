"use client";

import { useState } from "react";

import { Plus } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";

import { useMyCourses } from "../hooks/useMyCourses";
import { CreateCourseDialog } from "./CreateCourseDialog";
import { MyCoursesTable } from "./MyCoursesTable";

const SKELETON_ROW_COUNT = 3;

export function MyCourses() {
    const { data, error, isPending, refetch } = useMyCourses();
    const [isCreateOpen, setIsCreateOpen] = useState(false);

    return (
        <div className="mx-auto flex max-w-[1280px] flex-col gap-5 px-6 pt-7 pb-16">
            <div className="flex flex-wrap items-end justify-between gap-3">
                <div>
                    <h1 className="text-[28px] font-extrabold tracking-[-0.02em]">
                        Khoá học của tôi
                    </h1>
                    {data ? (
                        <p className="mt-1 text-muted-foreground">
                            {formatNumber(data.length)} khoá
                        </p>
                    ) : null}
                </div>
                <Button type="button" size="lg" onClick={() => setIsCreateOpen(true)}>
                    <Plus aria-hidden="true" />
                    Tạo khoá mới
                </Button>
            </div>
            <CreateCourseDialog open={isCreateOpen} onOpenChange={setIsCreateOpen} />
            {isPending ? (
                <div role="status" aria-label="Đang tải khoá học" className="flex flex-col gap-3">
                    {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-14" />
                    ))}
                </div>
            ) : null}
            {error && !data ? (
                <ErrorState
                    title="Không tải được khoá học"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {data && data.length === 0 ? <EmptyState title="Bạn chưa có khoá nào." /> : null}
            {data && data.length > 0 ? <MyCoursesTable courses={data} /> : null}
        </div>
    );
}
