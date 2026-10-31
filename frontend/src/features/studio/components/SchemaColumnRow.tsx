"use client";

import { Trash2 } from "lucide-react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import { LOGICAL_TYPE_LABELS, referenceOptions } from "../schema-editing";
import type { DatasetColumn, DatasetTable, LogicalType } from "../types";

const TYPES = Object.keys(LOGICAL_TYPE_LABELS) as LogicalType[];

interface SchemaColumnRowProps {
    column: DatasetColumn;
    label: string;
    path: string;
    tables: DatasetTable[];
    disabled: boolean;
    canRemove: boolean;
    errorOf: (path: string) => string | undefined;
    onChange: (change: Partial<DatasetColumn>) => void;
    onRemove: () => void;
}

function referenceValue(column: DatasetColumn): string {
    return column.references ? `${column.references.table}.${column.references.column}` : "";
}

function toReference(value: string): DatasetColumn["references"] {
    if (value === "") {
        return null;
    }
    const [table, ...columnParts] = value.split(".");
    return { table, column: columnParts.join(".") };
}

export function SchemaColumnRow({
    column,
    label,
    path,
    tables,
    disabled,
    canRemove,
    errorOf,
    onChange,
    onRemove,
}: SchemaColumnRowProps) {
    const nameError = errorOf(`${path}.name`);
    const referenceError = errorOf(`${path}.references`) ?? errorOf(`${path}.isNullable`);
    const current = referenceValue(column);
    const options = referenceOptions(tables, column.type);
    const choices = current && !options.includes(current) ? [current, ...options] : options;

    return (
        <li className="grid grid-cols-[minmax(140px,1fr)_150px_88px_100px_minmax(150px,1fr)_40px] items-start gap-2">
            <div className="flex flex-col gap-1">
                <Input
                    aria-label={`Tên cột ${label}`}
                    value={column.name}
                    disabled={disabled}
                    autoComplete="off"
                    className="font-mono"
                    aria-invalid={nameError ? true : undefined}
                    aria-describedby={nameError ? `${path}-name-error` : undefined}
                    onChange={(event) => onChange({ name: event.target.value })}
                />
                {nameError ? (
                    <p id={`${path}-name-error`} className="text-[13px] text-destructive">
                        {nameError}
                    </p>
                ) : null}
            </div>
            <NativeSelect
                aria-label={`Kiểu, cột ${label}`}
                value={column.type}
                disabled={disabled}
                onChange={(event) => onChange({ type: event.target.value as LogicalType })}
            >
                {TYPES.map((type) => (
                    <option key={type} value={type}>
                        {LOGICAL_TYPE_LABELS[type]}
                    </option>
                ))}
            </NativeSelect>
            <label className="flex h-9 items-center justify-center">
                <input
                    type="checkbox"
                    aria-label={`Khoá chính, cột ${label}`}
                    checked={column.isPrimaryKey}
                    disabled={disabled}
                    onChange={(event) =>
                        onChange({
                            isPrimaryKey: event.target.checked,
                            ...(event.target.checked ? { isNullable: false } : {}),
                        })
                    }
                />
            </label>
            <label className="flex h-9 items-center justify-center">
                <input
                    type="checkbox"
                    aria-label={`Cho phép rỗng, cột ${label}`}
                    checked={column.isNullable}
                    disabled={disabled || column.isPrimaryKey}
                    onChange={(event) => onChange({ isNullable: event.target.checked })}
                />
            </label>
            <div className="flex flex-col gap-1">
                <NativeSelect
                    aria-label={`Khoá ngoại, cột ${label}`}
                    value={current}
                    disabled={disabled}
                    className="font-mono"
                    aria-invalid={referenceError ? true : undefined}
                    onChange={(event) => onChange({ references: toReference(event.target.value) })}
                >
                    <option value="">Không</option>
                    {choices.map((choice) => (
                        <option key={choice} value={choice}>
                            {choice}
                        </option>
                    ))}
                </NativeSelect>
                {referenceError ? (
                    <p className="text-[13px] text-destructive">{referenceError}</p>
                ) : null}
            </div>
            <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label={`Xoá cột ${label}`}
                disabled={disabled || !canRemove}
                onClick={onRemove}
            >
                <Trash2 aria-hidden="true" />
            </Button>
        </li>
    );
}
