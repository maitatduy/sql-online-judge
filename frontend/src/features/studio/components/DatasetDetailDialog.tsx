"use client";

import Link from "next/link";

import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { useStudioDataset } from "../hooks/useStudioDataset";
import { LOGICAL_TYPE_LABELS } from "../schema-editing";
import type { StudioDataset, StudioDatasetSummary } from "../types";

interface DatasetDetailDialogProps {
    dataset: StudioDatasetSummary;
    onClose: () => void;
}

function SchemaSummary({ dataset }: { dataset: StudioDataset }) {
    return (
        <div className="flex flex-col gap-3">
            {dataset.schema.tables.length === 0 ? (
                <p className="text-[13px] text-muted-foreground">Chưa có bảng nào.</p>
            ) : null}
            {dataset.schema.tables.map((table) => (
                <section key={table.name} className="rounded-md border p-3">
                    <h3 className="font-mono text-sm font-bold">{table.name}</h3>
                    <ul className="mt-1.5 flex flex-col gap-1 text-[13px]">
                        {table.columns.map((column) => (
                            <li key={column.name} className="flex flex-wrap gap-x-2">
                                <span className="font-mono">{column.name}</span>
                                <span className="text-muted-foreground">
                                    {LOGICAL_TYPE_LABELS[column.type]}
                                    {column.isPrimaryKey ? " · Khoá chính" : ""}
                                    {column.references
                                        ? ` · Khoá ngoại tới ${column.references.table}.${column.references.column}`
                                        : ""}
                                </span>
                            </li>
                        ))}
                    </ul>
                </section>
            ))}
            <section>
                <h3 className="text-sm font-bold">Bộ dữ liệu</h3>
                {dataset.variants.length === 0 ? (
                    <p className="mt-1 text-[13px] text-muted-foreground">
                        Chưa có bộ dữ liệu nào.
                    </p>
                ) : (
                    <ul className="mt-1.5 flex flex-col gap-1 text-[13px]">
                        {dataset.variants.map((variant) => (
                            <li key={variant.id}>
                                {variant.name} · {variant.isPublic ? "Công khai" : "Ẩn"} ·{" "}
                                {formatNumber(
                                    variant.tables.reduce((sum, table) => sum + table.rowCount, 0),
                                )}{" "}
                                dòng
                            </li>
                        ))}
                    </ul>
                )}
            </section>
        </div>
    );
}

function DatasetDetailBody({ datasetId }: { datasetId: string }) {
    const { data, error } = useStudioDataset(datasetId);

    if (error) {
        return (
            <p className="text-[13px] text-destructive">Không tải được database. {error.message}</p>
        );
    }
    if (!data) {
        return (
            <div
                role="status"
                aria-label="Đang tải chi tiết database"
                className="flex flex-col gap-2"
            >
                <Skeleton className="h-4 w-40" />
                <Skeleton className="h-24" />
            </div>
        );
    }
    return <SchemaSummary dataset={data} />;
}

export function DatasetDetailDialog({ dataset, onClose }: DatasetDetailDialogProps) {
    return (
        <Dialog open onOpenChange={(open) => (open ? undefined : onClose())}>
            <DialogContent className="sm:max-w-2xl">
                <DialogTitle>{dataset.name}</DialogTitle>
                <DialogDescription>{dataset.description || "Chưa có mô tả."}</DialogDescription>
                <div className="max-h-[60vh] overflow-y-auto">
                    <DatasetDetailBody datasetId={dataset.id} />
                </div>
                <div className="flex justify-end gap-2">
                    <Button type="button" variant="outline" onClick={onClose}>
                        Đóng
                    </Button>
                    <Button asChild>
                        <Link href={ROUTES.studioDataset(dataset.id)}>Sửa database</Link>
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    );
}
