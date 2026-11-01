"use client";

import { useState } from "react";

import { ClipboardPaste, Plus, Trash2, Upload } from "lucide-react";

import { Button } from "@/components/ui/button";
import { ApiRequestError } from "@/lib/api-client";
import { useUnsavedChangesWarning } from "@/lib/useUnsavedChangesWarning";

import { type CsvCell, rowsFromCsv, rowsToCsv } from "../csv";
import { PasteCsvDialog } from "./PasteCsvDialog";
import { SaveBar } from "./SaveBar";

interface CsvSave {
    save: (csv: string) => void;
    isSaving: boolean;
    error: Error | null;
    savedAt: string | null;
}

interface VariantTableGridProps {
    columns: string[];
    initialRows: CsvCell[][];
    save: CsvSave;
}

function errorMessageOf(error: Error | null): string | null {
    if (!error) {
        return null;
    }
    return error instanceof ApiRequestError && error.errors.length > 0
        ? error.errors[0].message
        : error.message;
}

export function VariantTableGrid({ columns, initialRows, save }: VariantTableGridProps) {
    const [rows, setRows] = useState(initialRows);
    const [isPasteOpen, setIsPasteOpen] = useState(false);
    const [fileError, setFileError] = useState<string | null>(null);
    const hasChanges = JSON.stringify(rows) !== JSON.stringify(initialRows);
    const errorMessage = fileError ?? errorMessageOf(save.error);
    useUnsavedChangesWarning(hasChanges);

    function changeCell(rowIndex: number, columnIndex: number, value: string) {
        setRows(
            rows.map((row, at) =>
                at === rowIndex
                    ? row.map((cell, column) => (column === columnIndex ? value || null : cell))
                    : row,
            ),
        );
    }

    async function loadFile(file: File | undefined) {
        if (!file) {
            return;
        }
        const parsed = rowsFromCsv(await file.text(), columns);
        setFileError(parsed.error);
        if (!parsed.error) {
            setRows(parsed.rows);
        }
    }

    return (
        <div className="flex flex-col gap-3">
            <div className="flex flex-wrap gap-2">
                <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setIsPasteOpen(true)}
                >
                    <ClipboardPaste aria-hidden="true" />
                    Dán CSV
                </Button>
                <label className="inline-flex h-8 cursor-pointer items-center gap-1.5 rounded-md border px-3 text-sm font-medium hover:bg-surface-2 focus-within:ring-2 focus-within:ring-ring">
                    <Upload aria-hidden="true" className="size-4" />
                    Tải file .csv
                    <input
                        type="file"
                        accept=".csv,text/csv"
                        className="sr-only"
                        onChange={(event) => void loadFile(event.target.files?.[0])}
                    />
                </label>
            </div>
            <div className="overflow-x-auto rounded-md border">
                <table className="w-full min-w-[480px] border-collapse text-sm">
                    <thead>
                        <tr>
                            {columns.map((column) => (
                                <th
                                    key={column}
                                    scope="col"
                                    className="border-b px-2 py-2 text-left text-xs font-semibold text-muted-foreground"
                                >
                                    {column}
                                </th>
                            ))}
                            <th scope="col" className="w-10 border-b">
                                <span className="sr-only">Xoá dòng</span>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map((row, rowIndex) => (
                            <tr key={rowIndex}>
                                {columns.map((column, columnIndex) => (
                                    <td key={column} className="border-b p-1">
                                        <input
                                            aria-label={`${column}, dòng ${rowIndex + 1}`}
                                            value={row[columnIndex] ?? ""}
                                            placeholder="NULL"
                                            onChange={(event) =>
                                                changeCell(
                                                    rowIndex,
                                                    columnIndex,
                                                    event.target.value,
                                                )
                                            }
                                            className="h-8 w-full min-w-24 rounded-sm bg-transparent px-2 outline-none placeholder:text-muted-foreground/60 focus-visible:ring-2 focus-visible:ring-ring"
                                        />
                                    </td>
                                ))}
                                <td className="border-b p-1 text-center">
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon-sm"
                                        aria-label={`Xoá dòng ${rowIndex + 1}`}
                                        onClick={() =>
                                            setRows(rows.filter((_, at) => at !== rowIndex))
                                        }
                                    >
                                        <Trash2 aria-hidden="true" />
                                    </Button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {rows.length === 0 ? (
                    <p className="px-3 py-4 text-center text-[13px] text-muted-foreground">
                        Chưa có dòng nào.
                    </p>
                ) : null}
            </div>
            <div>
                <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => setRows([...rows, columns.map(() => null)])}
                >
                    <Plus aria-hidden="true" />
                    Thêm dòng
                </Button>
            </div>
            {errorMessage ? (
                <p role="alert" className="text-[13px] text-destructive">
                    {errorMessage}
                </p>
            ) : null}
            <SaveBar
                hasChanges={hasChanges}
                canSave={hasChanges}
                isSaving={save.isSaving}
                savedAt={save.savedAt}
                onSave={() => save.save(rowsToCsv(columns, rows))}
            />
            <PasteCsvDialog
                open={isPasteOpen}
                columns={columns}
                onOpenChange={setIsPasteOpen}
                onApply={(pastedRows) => {
                    setFileError(null);
                    setRows(pastedRows);
                }}
            />
        </div>
    );
}
