"use client";

import { useState } from "react";

import Link from "next/link";

import { DeleteConfirmDialog } from "@/components/shared/DeleteConfirmDialog";
import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import type { AdminCourseAction, AdminCourseItem } from "../../courses/types";
import { useCourseVisibility } from "../../hooks/useAdminCourses";

const CELL_CLASS = "border-b px-4 py-3 align-middle";

function statusLabelOf(course: AdminCourseItem): string {
    if (course.status === "ARCHIVED") {
        return "Đã ẩn";
    }
    if (course.revisionStatus === "IN_REVIEW") {
        return "Chờ duyệt";
    }
    return course.status === "PUBLISHED" ? "Đã xuất bản" : "Bản nháp";
}

interface AdminCourseRowProps {
    course: AdminCourseItem;
    now: Date;
}

export function AdminCourseRow({ course, now }: AdminCourseRowProps) {
    const showToast = useToast();
    const visibility = useCourseVisibility();
    const [isConfirming, setIsConfirming] = useState(false);

    function change(action: AdminCourseAction) {
        void visibility
            .mutateAsync({ courseId: course.courseId, action })
            .then(() =>
                showToast(
                    action === "archive"
                        ? `Đã ẩn khoá ${course.title}.`
                        : `Đã hiện lại khoá ${course.title}.`,
                ),
            )
            .catch((error: Error) => showToast(error.message, "error"))
            .finally(() => setIsConfirming(false));
    }

    return (
        <tr>
            <td className={CELL_CLASS}>
                <Link
                    href={ROUTES.studioCourse(course.courseId)}
                    className="font-semibold hover:text-brand hover:underline"
                >
                    {course.title}
                </Link>
                <p className="text-sm text-muted-foreground">{course.slug}</p>
            </td>
            <td className={CELL_CLASS}>
                <span className="inline-flex h-6 items-center rounded-full bg-surface-2 px-2.5 text-sm font-semibold text-muted-foreground">
                    {statusLabelOf(course)}
                </span>
            </td>
            <td className={CELL_CLASS}>
                <DifficultyLabel difficulty={course.difficulty} className="text-sm" />
            </td>
            <td className={`${CELL_CLASS} text-sm`}>{formatNumber(course.lessonCount)}</td>
            <td className={`${CELL_CLASS} text-sm text-muted-foreground`}>
                <time dateTime={course.updatedAt}>{formatRelativeTime(course.updatedAt, now)}</time>
            </td>
            <td className={`${CELL_CLASS} py-1.5`}>
                {course.status === "PUBLISHED" ? (
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={visibility.isPending}
                        onClick={() => setIsConfirming(true)}
                    >
                        Ẩn
                    </Button>
                ) : null}
                {course.status === "ARCHIVED" ? (
                    <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={visibility.isPending}
                        onClick={() => change("restore")}
                    >
                        Hiện lại
                    </Button>
                ) : null}
                <DeleteConfirmDialog
                    open={isConfirming}
                    onOpenChange={setIsConfirming}
                    title={`Ẩn khoá ${course.title}?`}
                    description="Khoá biến khỏi trang học. Tiến độ của học viên vẫn được giữ, có thể hiện lại sau."
                    confirmLabel="Ẩn khoá"
                    isPending={visibility.isPending}
                    onConfirm={() => change("archive")}
                />
            </td>
        </tr>
    );
}
