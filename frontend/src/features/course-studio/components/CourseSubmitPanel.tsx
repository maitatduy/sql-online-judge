"use client";

import { useState } from "react";

import { Send } from "lucide-react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { formatRelativeTime } from "@/lib/format";

import { useSubmitCourse } from "../hooks/useStudioCourse";
import type { StudioCourse } from "../types";

interface CourseSubmitPanelProps {
    course: StudioCourse;
}

export function CourseSubmitPanel({ course }: CourseSubmitPanelProps) {
    const showToast = useToast();
    const submitCourse = useSubmitCourse(course.id);
    const [now] = useState(() => new Date());
    const review = course.latestReview;

    if (course.revisionStatus === "IN_REVIEW") {
        return (
            <p className="rounded-lg border bg-surface-2 px-4 py-3">
                Khoá đang chờ admin duyệt
                {course.submittedAt ? `, gửi ${formatRelativeTime(course.submittedAt, now)}` : ""}.
            </p>
        );
    }
    if (course.revisionStatus !== "DRAFT") {
        return null;
    }

    function submit() {
        void submitCourse
            .mutateAsync()
            .then(() => showToast("Đã gửi khoá đi duyệt."))
            .catch((error: Error) => showToast(`Chưa gửi được khoá. ${error.message}`, "error"));
    }

    const isEmpty = course.lessonCount === 0;
    return (
        <section
            aria-label="Gửi duyệt"
            className="flex flex-col gap-3 rounded-lg border bg-card p-4 sm:flex-row sm:items-center"
        >
            <div className="flex-1">
                {review?.decision === "CHANGES_REQUESTED" ? (
                    <>
                        <p className="font-semibold">Admin yêu cầu sửa</p>
                        <p className="text-muted-foreground">{review.reason}</p>
                    </>
                ) : (
                    <p className="text-muted-foreground">
                        {course.status === "PUBLISHED"
                            ? "Đây là bản sửa. Học viên vẫn học bản đang xuất bản cho tới khi bản này được duyệt."
                            : "Soạn xong thì gửi duyệt để khoá được xuất bản."}
                    </p>
                )}
                {isEmpty ? (
                    <p className="text-sm text-destructive">
                        Thêm ít nhất một bài giảng để gửi duyệt.
                    </p>
                ) : null}
            </div>
            <Button
                type="button"
                size="lg"
                disabled={isEmpty || submitCourse.isPending}
                onClick={submit}
            >
                <Send aria-hidden="true" />
                Gửi duyệt
            </Button>
        </section>
    );
}
