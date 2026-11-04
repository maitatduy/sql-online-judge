"use client";

import { type Dispatch, type SetStateAction, useState } from "react";

import { Plus } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import type { CsvCell } from "../csv";
import { useDatasetVariants } from "../hooks/useDatasetVariants";
import { useSaveVariantDrafts, VariantTableSaveError } from "../hooks/useVariantCsv";
import type { DatasetVariant, StudioDataset } from "../types";
import {
    columnsOf,
    draftKey,
    liveDrafts,
    type VariantDrafts,
    withoutDraft,
} from "../variant-drafts";
import { DeleteConfirmDialog } from "./DeleteConfirmDialog";
import { SaveBar } from "./SaveBar";
import { VariantCard } from "./VariantCard";
import { VariantDialog } from "./VariantDialog";
import { VariantTableEditor } from "./VariantTableEditor";

const NEW_VARIANT = { name: "", isPublic: false };

interface SelectedTable {
    variantId: string;
    tableName: string;
}

interface DatasetVariantsProps {
    dataset: StudioDataset;
    drafts: VariantDrafts;
    onDraftsChange: Dispatch<SetStateAction<VariantDrafts>>;
}

function saveErrorMessageOf(error: Error | null, dataset: StudioDataset): string | null {
    if (!error) {
        return null;
    }
    if (!(error instanceof VariantTableSaveError)) {
        return `Chưa lưu được dữ liệu. ${error.message}`;
    }
    const variantName =
        dataset.variants.find((variant) => variant.id === error.draft.variantId)?.name ?? "";
    return `Chưa lưu được bảng ${error.draft.tableName}, bộ ${variantName}. ${error.message}`;
}

export function DatasetVariants({ dataset, drafts, onDraftsChange }: DatasetVariantsProps) {
    const showToast = useToast();
    const { addVariant, updateVariant, deleteVariant } = useDatasetVariants(dataset.id);
    const [isAddOpen, setIsAddOpen] = useState(false);
    const [editing, setEditing] = useState<DatasetVariant | null>(null);
    const [deleting, setDeleting] = useState<DatasetVariant | null>(null);
    const [selected, setSelected] = useState<SelectedTable | null>(null);
    const selectedVariant = dataset.variants.find((variant) => variant.id === selected?.variantId);
    const pendingDrafts = liveDrafts(drafts, dataset);
    const save = useSaveVariantDrafts(dataset.id, (draft) =>
        onDraftsChange((current) =>
            withoutDraft(current, draftKey(draft.variantId, draft.tableName)),
        ),
    );
    const saveErrorMessage = saveErrorMessageOf(save.error, dataset);

    function draftRowsOf(variantId: string, tableName: string): CsvCell[][] | undefined {
        return pendingDrafts.find(
            (draft) => draft.variantId === variantId && draft.tableName === tableName,
        )?.rows;
    }

    function draftRowCountsOf(variantId: string): Record<string, number> {
        return Object.fromEntries(
            pendingDrafts
                .filter((draft) => draft.variantId === variantId)
                .map((draft) => [draft.tableName, draft.rows.length]),
        );
    }

    function changeDraft({ variantId, tableName }: SelectedTable, rows: CsvCell[][] | null) {
        const key = draftKey(variantId, tableName);
        onDraftsChange((current) =>
            rows
                ? {
                      ...current,
                      [key]: { variantId, tableName, columns: columnsOf(dataset, tableName), rows },
                  }
                : withoutDraft(current, key),
        );
    }

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
                    <VariantCard
                        key={variant.id}
                        variant={variant}
                        tables={dataset.schema.tables}
                        selectedTable={
                            selected?.variantId === variant.id ? selected.tableName : null
                        }
                        draftRowCounts={draftRowCountsOf(variant.id)}
                        onSelectTable={(tableName) =>
                            setSelected({ variantId: variant.id, tableName })
                        }
                        onEdit={() => setEditing(variant)}
                        onDelete={() => setDeleting(variant)}
                    />
                ))}
            </ul>
            {selected && selectedVariant ? (
                <VariantTableEditor
                    key={`${selected.variantId}/${selected.tableName}`}
                    dataset={dataset}
                    variant={selectedVariant}
                    tableName={selected.tableName}
                    draftRows={draftRowsOf(selected.variantId, selected.tableName)}
                    onDraftChange={(rows) => changeDraft(selected, rows)}
                    onClose={() => setSelected(null)}
                />
            ) : null}
            {dataset.variants.length > 0 ? (
                <div className="sticky bottom-0 flex flex-col gap-2 rounded-lg border bg-card px-4 py-3">
                    {saveErrorMessage ? (
                        <p role="alert" className="text-[13px] text-destructive">
                            {saveErrorMessage}
                        </p>
                    ) : null}
                    <SaveBar
                        hasChanges={pendingDrafts.length > 0}
                        canSave={pendingDrafts.length > 0}
                        isSaving={save.isSaving}
                        savedAt={save.savedAt}
                        onSave={() => save.save(pendingDrafts)}
                    />
                </div>
            ) : null}
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
