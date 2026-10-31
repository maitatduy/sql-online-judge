"use client";

import { Plus, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import {
    addColumn,
    columnPath,
    removeColumn,
    removeTable,
    renameTable,
    tablePath,
    updateColumn,
} from "../schema-editing";
import type { DatasetTable } from "../types";
import { SchemaColumnRow } from "./SchemaColumnRow";

const COLUMN_HEADERS = ["Tên cột", "Kiểu", "Khoá chính", "Cho phép rỗng", "Khoá ngoại"];

interface SchemaTableCardProps {
    tables: DatasetTable[];
    tableIndex: number;
    disabled: boolean;
    errorOf: (path: string) => string | undefined;
    onTablesChange: (tables: DatasetTable[]) => void;
}

export function SchemaTableCard({
    tables,
    tableIndex,
    disabled,
    errorOf,
    onTablesChange,
}: SchemaTableCardProps) {
    const table = tables[tableIndex];
    const tableLabel = table.name || `số ${tableIndex + 1}`;
    const path = tablePath(tableIndex);
    const nameError = errorOf(`${path}.name`);

    return (
        <section className="rounded-lg border bg-card p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="flex w-full max-w-xs flex-col gap-1">
                    <Input
                        aria-label={`Tên bảng ${tableIndex + 1}`}
                        value={table.name}
                        disabled={disabled}
                        autoComplete="off"
                        className="font-mono font-semibold"
                        aria-invalid={nameError ? true : undefined}
                        aria-describedby={nameError ? `${path}-name-error` : undefined}
                        onChange={(event) =>
                            onTablesChange(renameTable(tables, tableIndex, event.target.value))
                        }
                    />
                    {nameError ? (
                        <p id={`${path}-name-error`} className="text-[13px] text-destructive">
                            {nameError}
                        </p>
                    ) : null}
                </div>
                <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={disabled}
                    onClick={() => onTablesChange(removeTable(tables, tableIndex))}
                >
                    <Trash2 aria-hidden="true" />
                    <span className="sr-only">Xoá bảng {tableLabel}</span>
                    <span aria-hidden="true">Xoá bảng</span>
                </Button>
            </div>
            <div className="mt-4 overflow-x-auto">
                <div className="min-w-[760px]">
                    <div
                        aria-hidden="true"
                        className="grid grid-cols-[minmax(140px,1fr)_150px_88px_100px_minmax(150px,1fr)_40px] gap-2 pb-1.5 text-xs font-semibold text-muted-foreground"
                    >
                        {COLUMN_HEADERS.map((header) => (
                            <span key={header}>{header}</span>
                        ))}
                    </div>
                    <ul className="flex flex-col gap-2">
                        {table.columns.map((column, columnIndex) => (
                            <SchemaColumnRow
                                key={columnIndex}
                                column={column}
                                label={`${columnIndex + 1}, bảng ${tableLabel}`}
                                path={columnPath(tableIndex, columnIndex)}
                                tables={tables}
                                disabled={disabled}
                                canRemove={table.columns.length > 1}
                                errorOf={errorOf}
                                onChange={(change) =>
                                    onTablesChange(
                                        updateColumn(tables, tableIndex, columnIndex, change),
                                    )
                                }
                                onRemove={() =>
                                    onTablesChange(removeColumn(tables, tableIndex, columnIndex))
                                }
                            />
                        ))}
                    </ul>
                </div>
            </div>
            <Button
                type="button"
                variant="ghost"
                size="sm"
                className="mt-3"
                disabled={disabled}
                onClick={() => onTablesChange(addColumn(tables, tableIndex))}
            >
                <Plus aria-hidden="true" />
                <span className="sr-only">Thêm cột vào bảng {tableLabel}</span>
                <span aria-hidden="true">Thêm cột</span>
            </Button>
        </section>
    );
}
