"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";

import { type CsvCell, rowsFromCsv } from "../csv";

interface PasteCsvDialogProps {
    open: boolean;
    columns: string[];
    onOpenChange: (open: boolean) => void;
    onApply: (rows: CsvCell[][]) => void;
}

export function PasteCsvDialog({ open, columns, onOpenChange, onApply }: PasteCsvDialogProps) {
    const [csv, setCsv] = useState("");
    const [error, setError] = useState<string | null>(null);

    function changeOpen(nextOpen: boolean) {
        if (!nextOpen) {
            setCsv("");
            setError(null);
        }
        onOpenChange(nextOpen);
    }

    function apply() {
        const parsed = rowsFromCsv(csv, columns);
        if (parsed.error) {
            setError(parsed.error);
            return;
        }
        onApply(parsed.rows);
        changeOpen(false);
    }

    return (
        <Dialog open={open} onOpenChange={changeOpen}>
            <DialogContent className="sm:max-w-2xl">
                <DialogTitle>Dán CSV</DialogTitle>
                <DialogDescription>
                    Dòng đầu là tên cột: {columns.join(", ")}. Ô để trống được hiểu là NULL. Dữ liệu
                    dán vào sẽ thay toàn bộ các dòng đang có trong bảng.
                </DialogDescription>
                <textarea
                    aria-label="Nội dung CSV"
                    value={csv}
                    rows={10}
                    spellCheck={false}
                    aria-invalid={error ? true : undefined}
                    aria-describedby={error ? "paste-csv-error" : undefined}
                    onChange={(event) => setCsv(event.target.value)}
                    className="rounded-md border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
                />
                {error ? (
                    <p id="paste-csv-error" className="text-[13px] text-destructive">
                        {error}
                    </p>
                ) : null}
                <div className="flex justify-end gap-2">
                    <Button type="button" variant="outline" onClick={() => changeOpen(false)}>
                        Huỷ
                    </Button>
                    <Button type="button" onClick={apply}>
                        Dùng dữ liệu này
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    );
}
