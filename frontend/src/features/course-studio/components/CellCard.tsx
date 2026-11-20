"use client";

import { useState } from "react";

import { Pencil, Trash2 } from "lucide-react";

import { DeleteConfirmDialog } from "@/components/shared/DeleteConfirmDialog";
import { IconAction } from "@/components/shared/IconAction";
import { useToast } from "@/components/shared/ToastProvider";
import { DIALECT_LABELS } from "@/lib/dialect";

import { useDeleteCell } from "../hooks/useLessonCells";
import type { StudioCell } from "../types";
import { CellForm } from "./CellForm";

interface CellCardProps {
    lessonId: string;
    cell: StudioCell;
    index: number;
    isLocked: boolean;
}

export function CellCard({ lessonId, cell, index, isLocked }: CellCardProps) {
    const showToast = useToast();
    const deleteCell = useDeleteCell(lessonId);
    const [isEditing, setIsEditing] = useState(false);
    const [isConfirming, setIsConfirming] = useState(false);
    const label = `Ô SQL ${index}`;

    function confirmDelete() {
        void deleteCell
            .mutateAsync(cell.id)
            .catch((error: Error) => showToast(error.message, "error"))
            .finally(() => setIsConfirming(false));
    }

    return (
        <div role="group" aria-label={label} className="rounded-lg border bg-card">
            <div className="flex min-h-12 items-center gap-2 border-b px-4 py-2">
                <h3 className="flex-1 font-bold">{label}</h3>
                <span className="rounded-full bg-surface-2 px-2.5 py-0.5 text-sm font-semibold text-muted-foreground">
                    {DIALECT_LABELS[cell.dialect]}
                </span>
                {isLocked ? null : (
                    <>
                        <IconAction
                            label={`Sửa ô SQL ${index}`}
                            tooltip="Sửa"
                            icon={Pencil}
                            onClick={() => setIsEditing(true)}
                        />
                        <IconAction
                            label={`Xoá ô SQL ${index}`}
                            tooltip="Xoá"
                            icon={Trash2}
                            onClick={() => setIsConfirming(true)}
                        />
                    </>
                )}
            </div>
            <div className="p-4">
                {isEditing ? (
                    <CellForm lessonId={lessonId} cell={cell} onDone={() => setIsEditing(false)} />
                ) : (
                    <pre className="overflow-x-auto rounded-md bg-surface-2 p-3 font-mono text-sm">
                        {cell.starterSql}
                    </pre>
                )}
            </div>
            <DeleteConfirmDialog
                open={isConfirming}
                onOpenChange={setIsConfirming}
                title={`Xoá ô SQL ${index}?`}
                description="Học viên sẽ không còn chạy thử được ô này."
                confirmLabel="Xoá ô SQL"
                isPending={deleteCell.isPending}
                onConfirm={confirmDelete}
            />
        </div>
    );
}
