"use client";

import { useState } from "react";

import { ClipboardPaste, Plus, Trash2, Upload } from "lucide-react";

import { Button } from "@/components/ui/button";

import { type CsvCell, rowsFromCsv } from "../csv";
import { GridPager } from "./GridPager";
import { PasteCsvDialog } from "./PasteCsvDialog";

const PAGE_SIZE = 20;

interface VariantTableGridProps {
    columns: string[];
    rows: CsvCell[][];
    onRowsChange: (rows: CsvCell[][]) => void;
}

export function VariantTableGrid({ columns, rows, onRowsChange: setRows }: VariantTableGridProps) {
    const [isPasteOpen, setIsPasteOpen] = useState(false);
    const [fileError, setFileError] = useState<string | null>(null);
    const [page, setPage] = useState(1);
    const currentPage = Math.min(page, Math.max(1, Math.ceil(rows.length / PAGE_SIZE)));
    const start = (currentPage - 1) * PAGE_SIZE;
    const visibleRows = rows
        .slice(start, start + PAGE_SIZE)
        .map((row, offset) => ({ row, rowIndex: start + offset }));

    function changeCell(rowIndex: number, columnIndex: number, value: string) {
        setRows(
            rows.map((row, at) =>
                at === rowIndex
                    ? row.map((cell, column) => (column === columnIndex ? value || null : cell))
                    : row,
            ),
        );
    }

    function addRow() {
        const next = [...rows, columns.map(() => null)];
        setRows(next);
        setPage(Math.ceil(next.length / PAGE_SIZE));
    }

    async function loadFile(file: File | undefined) {
        if (!file) {
            return;
        }
        const parsed = rowsFromCsv(await file.text(), columns);
        setFileError(parsed.error);
        if (!parsed.error) {
            setRows(parsed.rows);
            setPage(1);
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
                        {visibleRows.map(({ row, rowIndex }) => (
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
            {rows.length > 0 ? (
                <GridPager
                    page={currentPage}
                    pageSize={PAGE_SIZE}
                    totalRows={rows.length}
                    onChange={setPage}
                />
            ) : null}
            <div>
                <Button type="button" variant="ghost" size="sm" onClick={addRow}>
                    <Plus aria-hidden="true" />
                    Thêm dòng
                </Button>
            </div>
            {fileError ? (
                <p role="alert" className="text-[13px] text-destructive">
                    {fileError}
                </p>
            ) : null}
            <PasteCsvDialog
                open={isPasteOpen}
                columns={columns}
                onOpenChange={setIsPasteOpen}
                onApply={(pastedRows) => {
                    setFileError(null);
                    setRows(pastedRows);
                    setPage(1);
                }}
            />
        </div>
    );
}
