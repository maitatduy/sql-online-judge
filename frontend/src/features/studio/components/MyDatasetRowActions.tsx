"use client";

import { useState } from "react";

import { Eye, Pencil, Trash2 } from "lucide-react";

import { IconAction } from "@/components/shared/IconAction";
import { useToast } from "@/components/shared/ToastProvider";
import { ROUTES } from "@/lib/routes";

import { useDeleteDataset } from "../hooks/useMyDatasets";
import type { StudioDatasetSummary } from "../types";
import { DatasetDetailDialog } from "./DatasetDetailDialog";
import { DeleteConfirmDialog } from "./DeleteConfirmDialog";

interface MyDatasetRowActionsProps {
    dataset: StudioDatasetSummary;
}

export function MyDatasetRowActions({ dataset }: MyDatasetRowActionsProps) {
    const showToast = useToast();
    const deleteDataset = useDeleteDataset();
    const [isDetailOpen, setIsDetailOpen] = useState(false);
    const [isDeleteOpen, setIsDeleteOpen] = useState(false);

    function confirmDelete() {
        deleteDataset.mutate(dataset.id, {
            onSuccess: () => showToast(`Đã xoá dataset ${dataset.name}.`),
            onError: (error) => showToast(`Chưa xoá được dataset. ${error.message}`, "error"),
            onSettled: () => setIsDeleteOpen(false),
        });
    }

    return (
        <div className="flex items-center gap-0.5">
            <IconAction
                label={`Xem chi tiết ${dataset.name}`}
                tooltip="Xem chi tiết"
                icon={Eye}
                onClick={() => setIsDetailOpen(true)}
            />
            <IconAction
                label={`Sửa ${dataset.name}`}
                tooltip="Sửa"
                icon={Pencil}
                href={ROUTES.studioDataset(dataset.id)}
            />
            <IconAction
                label={`Xoá ${dataset.name}`}
                tooltip="Xoá"
                icon={Trash2}
                onClick={() => setIsDeleteOpen(true)}
                className="hover:text-destructive"
            />
            {isDetailOpen ? (
                <DatasetDetailDialog dataset={dataset} onClose={() => setIsDetailOpen(false)} />
            ) : null}
            <DeleteConfirmDialog
                open={isDeleteOpen}
                onOpenChange={setIsDeleteOpen}
                title={`Xoá dataset ${dataset.name}?`}
                description="Chỉ xoá được dataset chưa bài nào dùng. Bạn không hoàn tác được trên giao diện."
                confirmLabel="Xoá dataset"
                isPending={deleteDataset.isPending}
                onConfirm={confirmDelete}
            />
        </div>
    );
}
