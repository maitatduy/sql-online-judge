"use client";

import { Pencil } from "lucide-react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import { useCreateCourseDraft } from "../hooks/useStudioCourse";
import type { StudioCourse } from "../types";

interface CoursePublishedPanelProps {
    course: StudioCourse;
}

export function CoursePublishedPanel({ course }: CoursePublishedPanelProps) {
    const showToast = useToast();
    const createDraft = useCreateCourseDraft(course.id);

    if (course.revisionStatus !== "PUBLISHED") {
        return null;
    }

    function startEditing() {
        void createDraft
            .mutateAsync()
            .then(() => showToast("Đã tạo bản sửa của khoá."))
            .catch((error: Error) => showToast(`Chưa tạo được bản sửa. ${error.message}`, "error"));
    }

    return (
        <section
            aria-label="Sửa khoá đã xuất bản"
            className="flex flex-col gap-3 rounded-lg border bg-card p-4 sm:flex-row sm:items-center"
        >
            <p className="flex-1 text-muted-foreground">
                Khoá đang xuất bản. Tạo bản sửa để chỉnh nội dung, học viên vẫn học bản hiện tại cho
                tới khi bản mới được duyệt.
            </p>
            <Button type="button" size="lg" disabled={createDraft.isPending} onClick={startEditing}>
                <Pencil aria-hidden="true" />
                Sửa khoá
            </Button>
        </section>
    );
}
