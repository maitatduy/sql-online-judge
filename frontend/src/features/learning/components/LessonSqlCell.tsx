"use client";

import { Play, RotateCcw } from "lucide-react";
import { useState } from "react";

import { LazySqlEditor } from "@/components/shared/LazySqlEditor";
import { SqlRunResult } from "@/components/shared/SqlRunResult";
import { Button } from "@/components/ui/button";
import { DIALECT_LABELS, type Dialect } from "@/lib/dialect";
import { cn } from "@/lib/utils";

import { useCellDraft } from "../hooks/useCellDraft";
import { useLessonRun } from "../hooks/useLessonRun";
import type { LessonCell } from "../types";

const DIALECTS = Object.keys(DIALECT_LABELS) as Dialect[];

interface LessonSqlCellProps {
    cell: LessonCell;
}

export function LessonSqlCell({ cell }: LessonSqlCellProps) {
    const { sql, isChanged, changeSql, reset } = useCellDraft(cell.cellKey, cell.starterSql);
    const { run, isRunning, error, startRun } = useLessonRun(cell.cellKey);
    const [dialect, setDialect] = useState<Dialect>(cell.dialect);
    const hasOutput = isRunning || error !== null || run !== null;

    return (
        <section aria-label="Ô SQL" className="overflow-hidden rounded-lg border bg-card">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b bg-surface px-3 py-2">
                <div
                    role="radiogroup"
                    aria-label="Hệ quản trị"
                    className="flex rounded-md border bg-card p-0.5"
                >
                    {DIALECTS.map((option) => (
                        <button
                            key={option}
                            type="button"
                            role="radio"
                            aria-checked={dialect === option}
                            onClick={() => setDialect(option)}
                            className={cn(
                                "rounded-sm px-3 py-1 text-[13px] font-semibold text-muted-foreground transition-colors hover:text-foreground",
                                dialect === option && "bg-brand-soft text-brand",
                            )}
                        >
                            {DIALECT_LABELS[option]}
                        </button>
                    ))}
                </div>
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
                        onClick={() => startRun({ sql, dialect })}
                    >
                        <Play aria-hidden="true" />
                        Chạy
                    </Button>
                </div>
            </div>
            <LazySqlEditor value={sql} onChange={changeSql} ariaLabel="Câu SQL" className="h-72" />
            {hasOutput ? (
                <div className="border-t px-4 py-3">
                    <SqlRunResult run={run} isRunning={isRunning} error={error} />
                </div>
            ) : null}
        </section>
    );
}
