"use client";

import { Play, RotateCcw } from "lucide-react";

import { LazySqlEditor } from "@/components/shared/LazySqlEditor";
import { SqlRunResult } from "@/components/shared/SqlRunResult";
import { Button } from "@/components/ui/button";

import { useCellDraft } from "../hooks/useCellDraft";
import { useLessonRun } from "../hooks/useLessonRun";
import type { LessonCell } from "../types";

const DIALECT_LABELS: Record<LessonCell["dialect"], string> = {
    POSTGRESQL: "PostgreSQL",
    MYSQL: "MySQL",
    SQLITE: "SQLite",
};

interface LessonSqlCellProps {
    cell: LessonCell;
}

export function LessonSqlCell({ cell }: LessonSqlCellProps) {
    const { sql, isChanged, changeSql, reset } = useCellDraft(cell.cellKey, cell.starterSql);
    const { run, isRunning, error, startRun } = useLessonRun(cell.cellKey);
    const hasOutput = isRunning || error !== null || run !== null;

    return (
        <section aria-label="Ô SQL" className="overflow-hidden rounded-lg border bg-card">
            <div className="flex items-center justify-between gap-2 border-b px-3 py-2">
                <span className="text-[12px] font-semibold text-muted-foreground">
                    {DIALECT_LABELS[cell.dialect]}
                </span>
                <div className="flex items-center gap-1.5">
                    {isChanged ? (
                        <Button type="button" size="sm" variant="ghost" onClick={reset}>
                            <RotateCcw aria-hidden="true" />
                            Đặt lại
                        </Button>
                    ) : null}
                    <Button
                        type="button"
                        size="sm"
                        disabled={isRunning || sql.trim() === ""}
                        onClick={() => startRun(sql)}
                    >
                        <Play aria-hidden="true" />
                        Chạy
                    </Button>
                </div>
            </div>
            <LazySqlEditor value={sql} onChange={changeSql} ariaLabel="Câu SQL" className="h-36" />
            {hasOutput ? (
                <div className="border-t px-4 py-3">
                    <SqlRunResult run={run} isRunning={isRunning} error={error} />
                </div>
            ) : null}
        </section>
    );
}
