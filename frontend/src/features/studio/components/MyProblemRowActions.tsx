"use client";

import { useState } from "react";

import { Eye, Pencil, Trash2 } from "lucide-react";

import { IconAction } from "@/components/shared/IconAction";
import { useToast } from "@/components/shared/ToastProvider";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { useDeleteProblem } from "../hooks/useDeleteProblem";
import { EDITABLE_STATUSES, type StudioProblemSummary } from "../types";
import { DeleteConfirmDialog } from "./DeleteConfirmDialog";
import { ProblemDetailDialog } from "./ProblemDetailDialog";

interface MyProblemRowActionsProps {
    problem: StudioProblemSummary;
}

export function MyProblemRowActions({ problem }: MyProblemRowActionsProps) {
    const showToast = useToast();
    const { data: currentUser } = useCurrentUser();
    const deleteProblem = useDeleteProblem();
    const [isDetailOpen, setIsDetailOpen] = useState(false);
    const [isDeleteOpen, setIsDeleteOpen] = useState(false);
    const isEditable = EDITABLE_STATUSES.includes(problem.status);
    const canDelete = isEditable || currentUser?.role === "ADMIN";

    function confirmDelete() {
        deleteProblem.mutate(problem.id, {
            onSuccess: () => {
                setIsDeleteOpen(false);
                showToast(`Đã xoá bài ${problem.title}.`);
            },
            onError: (error) => showToast(`Chưa xoá được bài. ${error.message}`, "error"),
        });
    }

    return (
        <div className="flex items-center gap-0.5">
            <IconAction
                label={`Xem chi tiết ${problem.title}`}
                tooltip="Xem chi tiết"
                icon={Eye}
                onClick={() => setIsDetailOpen(true)}
            />
            <IconAction
                label={`Sửa ${problem.title}`}
                tooltip={
                    isEditable ? "Sửa" : "Bài đang chờ duyệt hoặc đã xuất bản nên chưa sửa được"
                }
                icon={Pencil}
                href={`/studio/problems/${problem.id}`}
                disabled={!isEditable}
            />
            <IconAction
                label={`Xoá ${problem.title}`}
                tooltip={canDelete ? "Xoá" : "Chỉ xoá được bài nháp hoặc bài cần sửa"}
                icon={Trash2}
                disabled={!canDelete}
                onClick={() => setIsDeleteOpen(true)}
                className="hover:text-destructive"
            />
            {isDetailOpen ? (
                <ProblemDetailDialog problem={problem} onClose={() => setIsDetailOpen(false)} />
            ) : null}
            <DeleteConfirmDialog
                open={isDeleteOpen}
                onOpenChange={setIsDeleteOpen}
                title={`Xoá bài ${problem.title}?`}
                description="Bài sẽ biến mất khỏi Studio và trang luyện tập. Bạn không hoàn tác được trên giao diện."
                confirmLabel="Xoá bài"
                isPending={deleteProblem.isPending}
                onConfirm={confirmDelete}
            />
        </div>
    );
}
