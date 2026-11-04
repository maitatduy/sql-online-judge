"use client";

import { X } from "lucide-react";

import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";

import { type CsvCell, rowsFromCsv } from "../csv";
import { useVariantCsv } from "../hooks/useVariantCsv";
import type { DatasetVariant, StudioDataset } from "../types";
import { columnsOf } from "../variant-drafts";
import { VariantTableGrid } from "./VariantTableGrid";

interface VariantTableEditorProps {
    dataset: StudioDataset;
    variant: DatasetVariant;
    tableName: string;
    draftRows: CsvCell[][] | undefined;
    onDraftChange: (rows: CsvCell[][] | null) => void;
    onClose: () => void;
}

export function VariantTableEditor({
    dataset,
    variant,
    tableName,
    draftRows,
    onDraftChange,
    onClose,
}: VariantTableEditorProps) {
    const ref = { datasetId: dataset.id, variantId: variant.id, tableName };
    const columns = columnsOf(dataset, tableName);
    const hasData = variant.tables.some((table) => table.tableName === tableName);
    const { data, error, isPending } = useVariantCsv(ref, hasData);
    const savedCsv = hasData ? data?.csv : "";
    const savedRows =
        savedCsv === undefined ? undefined : savedCsv ? rowsFromCsv(savedCsv, columns).rows : [];

    function changeRows(savedRows: CsvCell[][], rows: CsvCell[][]) {
        onDraftChange(JSON.stringify(rows) === JSON.stringify(savedRows) ? null : rows);
    }

    return (
        <section
            aria-label={`Bảng ${tableName}, bộ ${variant.name}`}
            className="rounded-lg border bg-card p-4"
        >
            <div className="mb-3 flex items-center justify-between gap-2">
                <h3 className="text-sm font-bold">
                    Bảng {tableName}, bộ {variant.name}
                </h3>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon-sm"
                    aria-label="Đóng bảng"
                    onClick={onClose}
                >
                    <X aria-hidden="true" />
                </Button>
            </div>
            {hasData && isPending ? (
                <div role="status" aria-label="Đang tải dữ liệu" className="flex flex-col gap-2">
                    <Skeleton className="h-8" />
                    <Skeleton className="h-8" />
                </div>
            ) : null}
            {hasData && error ? (
                <p className="text-[13px] text-destructive">
                    Không tải được dữ liệu. {error.message}
                </p>
            ) : null}
            {savedRows ? (
                <VariantTableGrid
                    columns={columns}
                    rows={draftRows ?? savedRows}
                    onRowsChange={(rows) => changeRows(savedRows, rows)}
                />
            ) : null}
        </section>
    );
}
