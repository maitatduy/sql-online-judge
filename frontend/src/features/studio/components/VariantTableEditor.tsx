"use client";

import { X } from "lucide-react";

import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";

import { rowsFromCsv } from "../csv";
import { useSaveVariantCsv, useVariantCsv } from "../hooks/useVariantCsv";
import type { DatasetVariant, StudioDataset } from "../types";
import { VariantTableGrid } from "./VariantTableGrid";

interface VariantTableEditorProps {
    dataset: StudioDataset;
    variant: DatasetVariant;
    tableName: string;
    onClose: () => void;
}

export function VariantTableEditor({
    dataset,
    variant,
    tableName,
    onClose,
}: VariantTableEditorProps) {
    const ref = { datasetId: dataset.id, variantId: variant.id, tableName };
    const columns =
        dataset.schema.tables
            .find((table) => table.name === tableName)
            ?.columns.map((column) => column.name) ?? [];
    const hasData = variant.tables.some((table) => table.tableName === tableName);
    const { data, error, isPending } = useVariantCsv(ref, hasData);
    const save = useSaveVariantCsv(ref);
    const savedCsv = hasData ? data?.csv : "";

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
            {savedCsv !== undefined ? (
                <VariantTableGrid
                    key={savedCsv}
                    columns={columns}
                    initialRows={savedCsv ? rowsFromCsv(savedCsv, columns).rows : []}
                    save={save}
                />
            ) : null}
        </section>
    );
}
