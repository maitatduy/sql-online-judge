"use client";

import { useState } from "react";

import { BookOpen } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { NativeSelect } from "@/components/shared/NativeSelect";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";

import type { AdminCourseStatus } from "../../courses/types";
import { useAdminCourses } from "../../hooks/useAdminCourses";
import { AdminCourseRow } from "./AdminCourseRow";

const HEADER_CLASS = "border-b px-4 py-2.5 text-left text-sm font-semibold text-muted-foreground";
const SKELETON_ROW_COUNT = 4;
const STATUS_OPTIONS: { value: AdminCourseStatus; label: string }[] = [
    { value: "PUBLISHED", label: "Đã xuất bản" },
    { value: "DRAFT", label: "Bản nháp" },
    { value: "ARCHIVED", label: "Đã ẩn" },
];

export function AdminCourses() {
    const [status, setStatus] = useState<AdminCourseStatus | null>(null);
    const { data, error, isPending, refetch } = useAdminCourses(status);
    const [now] = useState(() => new Date());

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <div className="flex flex-wrap items-end justify-between gap-3">
                <div>
                    <h1 className="text-[24px] font-extrabold tracking-[-0.02em]">Khoá học</h1>
                    {data ? (
                        <p className="mt-1 text-muted-foreground">
                            {formatNumber(data.totalItems)} khoá
                        </p>
                    ) : null}
                </div>
                <NativeSelect
                    aria-label="Trạng thái"
                    value={status ?? ""}
                    onChange={(event) =>
                        setStatus((event.target.value || null) as AdminCourseStatus | null)
                    }
                    className="h-10 text-base"
                >
                    <option value="">Mọi trạng thái</option>
                    {STATUS_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </NativeSelect>
            </div>
            {isPending ? (
                <div role="status" aria-label="Đang tải khoá học" className="flex flex-col gap-3">
                    {Array.from({ length: SKELETON_ROW_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-14" />
                    ))}
                </div>
            ) : null}
            {error && !data ? (
                <ErrorState
                    title="Chưa tải được khoá học"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {data && data.items.length === 0 ? (
                <EmptyState title="Không có khoá nào." icon={<BookOpen />} />
            ) : null}
            {data && data.items.length > 0 ? (
                <div className="overflow-x-auto rounded-lg border bg-card">
                    <table aria-label="Khoá học" className="w-full min-w-[760px] border-collapse">
                        <thead>
                            <tr>
                                <th scope="col" className={HEADER_CLASS}>
                                    Khoá học
                                </th>
                                <th scope="col" className={HEADER_CLASS}>
                                    Trạng thái
                                </th>
                                <th scope="col" className={HEADER_CLASS}>
                                    Độ khó
                                </th>
                                <th scope="col" className={HEADER_CLASS}>
                                    Bài giảng
                                </th>
                                <th scope="col" className={HEADER_CLASS}>
                                    Cập nhật
                                </th>
                                <th scope="col" className={HEADER_CLASS}>
                                    Thao tác
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.items.map((course) => (
                                <AdminCourseRow key={course.courseId} course={course} now={now} />
                            ))}
                        </tbody>
                    </table>
                </div>
            ) : null}
        </main>
    );
}
