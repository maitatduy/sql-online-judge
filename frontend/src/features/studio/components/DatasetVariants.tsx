"use client";

import { useState } from "react";

import { Pencil, Plus, Trash2 } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { IconAction } from "@/components/shared/IconAction";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { useDatasetVariants } from "../hooks/useDatasetVariants";
import type { DatasetVariant, StudioDataset } from "../types";
import { DeleteConfirmDialog } from "./DeleteConfirmDialog";
import { VariantDialog } from "./VariantDialog";

const NEW_VARIANT = { name: "", isPublic: false };

interface DatasetVariantsProps {
    dataset: StudioDataset;
}

function rowCountLabel(variant: DatasetVariant, tableName: string): string {
    const table = variant.tables.find((candidate) => candidate.tableName === tableName);
    return table
        ? `${tableName} · ${formatNumber(table.rowCount)} dòng`
        : `${tableName} · chưa có dữ liệu`;
}

export function DatasetVariants({ dataset }: DatasetVariantsProps) {
    const showToast = useToast();
    const { addVariant, updateVariant, deleteVariant } = useDatasetVariants(dataset.id);
    const [isAddOpen, setIsAddOpen] = useState(false);
    const [editing, setEditing] = useState<DatasetVariant | null>(null);
    const [deleting, setDeleting] = useState<DatasetVariant | null>(null);

    function confirmDelete() {
        if (!deleting) {
            return;
        }
        const { id, name } = deleting;
        deleteVariant.mutate(id, {
            onSuccess: () => showToast(`Đã xoá bộ dữ liệu ${name}.`),
            onError: (error) => showToast(`Chưa xoá được bộ dữ liệu. ${error.message}`, "error"),
            onSettled: () => setDeleting(null),
        });
    }

    return (
        <section aria-label="Bộ dữ liệu" className="flex flex-col gap-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-[13px] text-muted-foreground">
                    Mỗi bộ dữ liệu là một bản dữ liệu đầy đủ cho mọi bảng trong schema.
                </p>
                <Button type="button" variant="outline" onClick={() => setIsAddOpen(true)}>
                    <Plus aria-hidden="true" />
                    Thêm bộ dữ liệu
                </Button>
            </div>
            {dataset.variants.length === 0 ? <EmptyState title="Chưa có bộ dữ liệu nào." /> : null}
            <ul className="grid gap-3 md:grid-cols-2">
                {dataset.variants.map((variant) => (
                    <li
                        key={variant.id}
                        aria-label={variant.name}
                        className="rounded-lg border bg-card p-4"
                    >
                        <div className="flex items-start justify-between gap-2">
                            <div className="flex flex-wrap items-center gap-2">
                                <p className="font-semibold">{variant.name}</p>
                                <span
                                    className={cn(
                                        "rounded-full px-2.5 py-0.5 text-xs font-semibold",
                                        variant.isPublic
                                            ? "bg-brand-soft text-brand"
                                            : "bg-surface-2",
                                    )}
                                >
                                    {variant.isPublic ? "Công khai" : "Ẩn"}
                                </span>
                            </div>
                            <div className="flex items-center gap-0.5">
                                <IconAction
                                    label={`Sửa ${variant.name}`}
                                    tooltip="Đổi tên, công khai hay ẩn"
                                    icon={Pencil}
                                    onClick={() => setEditing(variant)}
                                />
                                <IconAction
                                    label={`Xoá ${variant.name}`}
                                    tooltip="Xoá"
                                    icon={Trash2}
                                    onClick={() => setDeleting(variant)}
                                    className="hover:text-destructive"
                                />
                            </div>
                        </div>
                        <ul className="mt-2 flex flex-col gap-0.5 font-mono text-[13px] text-muted-foreground">
                            {dataset.schema.tables.map((table) => (
                                <li key={table.name}>{rowCountLabel(variant, table.name)}</li>
                            ))}
                        </ul>
                    </li>
                ))}
            </ul>
            <VariantDialog
                open={isAddOpen}
                title="Thêm bộ dữ liệu"
                submitLabel="Thêm"
                initialValues={NEW_VARIANT}
                isPending={addVariant.isPending}
                onOpenChange={setIsAddOpen}
                onSubmit={(values) => addVariant.mutateAsync(values)}
            />
            <VariantDialog
                open={editing !== null}
                title="Sửa bộ dữ liệu"
                submitLabel="Lưu"
                initialValues={editing ?? NEW_VARIANT}
                isPending={updateVariant.isPending}
                onOpenChange={(open) => (open ? undefined : setEditing(null))}
                onSubmit={(values) =>
                    updateVariant.mutateAsync({ variantId: editing?.id ?? "", variant: values })
                }
            />
            <DeleteConfirmDialog
                open={deleting !== null}
                onOpenChange={(open) => (open ? undefined : setDeleting(null))}
                title={`Xoá bộ dữ liệu ${deleting?.name ?? ""}?`}
                description="Dữ liệu của mọi bảng trong bộ này sẽ bị xoá theo."
                confirmLabel="Xoá bộ dữ liệu"
                isPending={deleteVariant.isPending}
                onConfirm={confirmDelete}
            />
        </section>
    );
}
