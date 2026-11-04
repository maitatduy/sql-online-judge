import { Pencil, Trash2 } from "lucide-react";

import { IconAction } from "@/components/shared/IconAction";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { DatasetTable, DatasetVariant } from "../types";

interface VariantCardProps {
    variant: DatasetVariant;
    tables: DatasetTable[];
    selectedTable: string | null;
    draftRowCounts: Record<string, number>;
    onSelectTable: (tableName: string) => void;
    onEdit: () => void;
    onDelete: () => void;
}

function rowCountLabel(
    variant: DatasetVariant,
    tableName: string,
    draftRowCount: number | undefined,
): string {
    if (draftRowCount !== undefined) {
        return `${tableName}: ${formatNumber(draftRowCount)} dòng, chưa lưu`;
    }
    const table = variant.tables.find((candidate) => candidate.tableName === tableName);
    return table
        ? `${tableName}: ${formatNumber(table.rowCount)} dòng`
        : `${tableName}: chưa có dữ liệu`;
}

export function VariantCard({
    variant,
    tables,
    selectedTable,
    draftRowCounts,
    onSelectTable,
    onEdit,
    onDelete,
}: VariantCardProps) {
    return (
        <li aria-label={variant.name} className="rounded-lg border bg-card p-4">
            <div className="flex items-start justify-between gap-2">
                <div className="flex flex-wrap items-center gap-2">
                    <p className="font-semibold">{variant.name}</p>
                    <span
                        className={cn(
                            "rounded-full px-2.5 py-0.5 text-xs font-semibold",
                            variant.isPublic ? "bg-brand-soft text-brand" : "bg-surface-2",
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
                        onClick={onEdit}
                    />
                    <IconAction
                        label={`Xoá ${variant.name}`}
                        tooltip="Xoá"
                        icon={Trash2}
                        onClick={onDelete}
                        className="hover:text-destructive"
                    />
                </div>
            </div>
            <ul className="mt-3 flex flex-col gap-2 text-sm text-muted-foreground">
                {tables.map((table) => (
                    <li key={table.name}>
                        <button
                            type="button"
                            aria-label={`Nhập dữ liệu bảng ${table.name}`}
                            aria-pressed={selectedTable === table.name}
                            onClick={() => onSelectTable(table.name)}
                            className="rounded-sm text-left hover:text-brand hover:underline aria-pressed:font-semibold aria-pressed:text-brand"
                        >
                            {rowCountLabel(variant, table.name, draftRowCounts[table.name])}
                        </button>
                    </li>
                ))}
            </ul>
        </li>
    );
}
