"use client";

import { useState } from "react";

import Link from "next/link";

import { Pencil, Plus, Trash2 } from "lucide-react";

import { DeleteConfirmDialog } from "@/components/shared/DeleteConfirmDialog";
import { IconAction } from "@/components/shared/IconAction";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { useDeleteChapter, useDeleteLesson } from "../hooks/useCourseOutline";
import type { StudioChapter, StudioLessonOutline } from "../types";
import { ChapterTitleForm } from "./ChapterTitleForm";
import { NewLessonForm } from "./NewLessonForm";

type PendingDelete = { kind: "chapter" } | { kind: "lesson"; lesson: StudioLessonOutline };

interface ChapterCardProps {
    courseId: string;
    chapter: StudioChapter;
    isLocked: boolean;
}

export function ChapterCard({ courseId, chapter, isLocked }: ChapterCardProps) {
    const showToast = useToast();
    const deleteChapter = useDeleteChapter();
    const deleteLesson = useDeleteLesson();
    const [isRenaming, setIsRenaming] = useState(false);
    const [isAdding, setIsAdding] = useState(false);
    const [pendingDelete, setPendingDelete] = useState<PendingDelete | null>(null);

    function confirmDelete() {
        const request =
            pendingDelete?.kind === "lesson"
                ? deleteLesson.mutateAsync(pendingDelete.lesson.id)
                : deleteChapter.mutateAsync(chapter.id);
        void request
            .catch((error: Error) => showToast(error.message, "error"))
            .finally(() => setPendingDelete(null));
    }

    return (
        <div role="group" aria-label={chapter.title} className="rounded-lg border bg-card">
            <div className="flex min-h-14 items-center gap-2 border-b px-4 py-2">
                {isRenaming ? (
                    <ChapterTitleForm
                        chapterId={chapter.id}
                        title={chapter.title}
                        onDone={() => setIsRenaming(false)}
                    />
                ) : (
                    <>
                        <h3 className="flex-1 text-base font-bold">
                            <span className="mr-2 text-muted-foreground">{chapter.position}.</span>
                            {chapter.title}
                        </h3>
                        {isLocked ? null : (
                            <>
                                <IconAction
                                    label={`Đổi tên chương ${chapter.title}`}
                                    tooltip="Đổi tên"
                                    icon={Pencil}
                                    onClick={() => setIsRenaming(true)}
                                />
                                <IconAction
                                    label={`Xoá chương ${chapter.title}`}
                                    tooltip="Xoá chương"
                                    icon={Trash2}
                                    onClick={() => setPendingDelete({ kind: "chapter" })}
                                />
                            </>
                        )}
                    </>
                )}
            </div>
            <ul className="divide-y">
                {chapter.lessons.map((lesson) => (
                    <li key={lesson.id} className="flex items-center gap-3 px-4 py-2.5">
                        <div className="min-w-0 flex-1">
                            <Link
                                href={ROUTES.studioLesson(courseId, lesson.id)}
                                className="block truncate font-semibold hover:text-brand hover:underline"
                            >
                                {lesson.title}
                            </Link>
                            <p className="truncate text-sm text-muted-foreground">{lesson.slug}</p>
                        </div>
                        <span className="text-sm whitespace-nowrap text-muted-foreground">
                            {formatNumber(lesson.estimatedMinutes)} phút
                        </span>
                        {isLocked ? null : (
                            <IconAction
                                label={`Xoá bài giảng ${lesson.title}`}
                                tooltip="Xoá bài giảng"
                                icon={Trash2}
                                onClick={() => setPendingDelete({ kind: "lesson", lesson })}
                            />
                        )}
                    </li>
                ))}
            </ul>
            {isLocked ? null : (
                <div className="px-4 py-3">
                    {isAdding ? (
                        <NewLessonForm chapterId={chapter.id} onDone={() => setIsAdding(false)} />
                    ) : (
                        <Button type="button" variant="ghost" onClick={() => setIsAdding(true)}>
                            <Plus aria-hidden="true" />
                            Thêm bài giảng
                        </Button>
                    )}
                </div>
            )}
            <DeleteConfirmDialog
                open={pendingDelete !== null}
                onOpenChange={(open) => (open ? undefined : setPendingDelete(null))}
                title={
                    pendingDelete?.kind === "lesson"
                        ? `Xoá bài giảng ${pendingDelete.lesson.title}?`
                        : `Xoá chương ${chapter.title}?`
                }
                description={
                    pendingDelete?.kind === "lesson"
                        ? "Nội dung, ô SQL và quiz của bài sẽ bị xoá luôn."
                        : "Chỉ xoá được chương không còn bài giảng."
                }
                confirmLabel={pendingDelete?.kind === "lesson" ? "Xoá bài giảng" : "Xoá chương"}
                isPending={deleteChapter.isPending || deleteLesson.isPending}
                onConfirm={confirmDelete}
            />
        </div>
    );
}
