"use client";

import { useState } from "react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { type Dialect, DIALECT_LABELS } from "@/lib/dialect";

import { useAddCell, useUpdateCell } from "../hooks/useLessonCells";
import type { StudioCell } from "../types";
import { DatasetVariantPicker } from "./DatasetVariantPicker";

const DIALECTS: Dialect[] = ["POSTGRESQL", "MYSQL", "SQLITE"];
const MAX_SQL_LENGTH = 10_000;

interface CellFormProps {
    lessonId: string;
    cell?: StudioCell;
    onDone: () => void;
}

export function CellForm({ lessonId, cell, onDone }: CellFormProps) {
    const showToast = useToast();
    const addCell = useAddCell(lessonId);
    const updateCell = useUpdateCell(lessonId);
    const [datasetId, setDatasetId] = useState("");
    const [variantId, setVariantId] = useState(cell?.datasetVariantId ?? "");
    const [dialect, setDialect] = useState<Dialect>(cell?.dialect ?? "POSTGRESQL");
    const [starterSql, setStarterSql] = useState(cell?.starterSql ?? "");
    const idPrefix = `cell-${cell?.id ?? "new"}`;
    const isTooLong = starterSql.length > MAX_SQL_LENGTH;
    const canSave =
        variantId !== "" &&
        starterSql.trim() !== "" &&
        !isTooLong &&
        !addCell.isPending &&
        !updateCell.isPending;

    function changeDataset(nextDatasetId: string) {
        setDatasetId(nextDatasetId);
        setVariantId("");
    }

    function submitCell(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const request = { datasetVariantId: variantId, dialect, starterSql };
        const saving = cell
            ? updateCell.mutateAsync({ cellId: cell.id, request })
            : addCell.mutateAsync(request);
        void saving
            .then(onDone)
            .catch((error: Error) => showToast(`Chưa lưu được ô SQL. ${error.message}`, "error"));
    }

    return (
        <form
            noValidate
            onSubmit={submitCell}
            className="flex flex-col gap-3 rounded-md bg-surface-2 p-4"
        >
            {cell ? null : (
                <DatasetVariantPicker
                    idPrefix={idPrefix}
                    datasetId={datasetId}
                    variantId={variantId}
                    onDatasetChange={changeDataset}
                    onVariantChange={setVariantId}
                />
            )}
            <div className="flex flex-col gap-1.5 sm:w-60">
                <Label htmlFor={`${idPrefix}-dialect`} className="text-sm">
                    Dialect
                </Label>
                <NativeSelect
                    id={`${idPrefix}-dialect`}
                    value={dialect}
                    onChange={(event) => setDialect(event.target.value as Dialect)}
                    className="h-10 bg-card text-base"
                >
                    {DIALECTS.map((option) => (
                        <option key={option} value={option}>
                            {DIALECT_LABELS[option]}
                        </option>
                    ))}
                </NativeSelect>
            </div>
            <div className="flex flex-col gap-1.5">
                <Label htmlFor={`${idPrefix}-sql`} className="text-sm">
                    Câu SQL mở đầu
                </Label>
                <textarea
                    id={`${idPrefix}-sql`}
                    value={starterSql}
                    spellCheck={false}
                    aria-invalid={isTooLong ? true : undefined}
                    onChange={(event) => setStarterSql(event.target.value)}
                    className="min-h-28 rounded-md border border-input bg-card p-3 font-mono text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
                />
                {isTooLong ? <p className="text-sm text-destructive">Tối đa 10.000 ký tự</p> : null}
            </div>
            <div className="flex justify-end gap-2">
                <Button type="button" variant="ghost" onClick={onDone}>
                    Huỷ
                </Button>
                <Button type="submit" disabled={!canSave}>
                    Lưu ô SQL
                </Button>
            </div>
        </form>
    );
}
