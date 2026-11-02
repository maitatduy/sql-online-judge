"use client";

import { useState } from "react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { type Dialect, DIALECT_LABELS } from "@/lib/dialect";

import { useCreateStudioRun, useStudioRun } from "../hooks/useStudioRun";
import type { StudioProblem, StudioRun } from "../types";
import { VariantRunCard } from "./VariantRunCard";

const DIALECTS = Object.keys(DIALECT_LABELS) as Dialect[];
const SYSTEM_ERROR_MESSAGE = "Máy chấm chưa chạy được câu lệnh này. Thử lại sau.";

interface QueryRunPanelProps {
    problem: StudioProblem;
}

function RunResults({ run }: { run: StudioRun }) {
    if (run.rejection) {
        return (
            <p role="alert" className="text-[13px] text-destructive">
                {`${run.rejection.verdict}: ${run.rejection.message}`}
            </p>
        );
    }
    if (run.systemError) {
        return (
            <p role="alert" className="text-[13px] text-destructive">
                {SYSTEM_ERROR_MESSAGE}
            </p>
        );
    }
    return (
        <div className="flex flex-col gap-3">
            {run.variants.map((result) => (
                <VariantRunCard key={result.variantId} result={result} />
            ))}
        </div>
    );
}

export function QueryRunPanel({ problem }: QueryRunPanelProps) {
    const [dialect, setDialect] = useState<Dialect>("POSTGRESQL");
    const [sql, setSql] = useState("");
    const [runId, setRunId] = useState<string | null>(null);
    const createRun = useCreateStudioRun(problem.id);
    const { data: run, error: runError } = useStudioRun(problem.id, runId);
    const hasDataset = problem.dataset !== null;
    const isWaiting = runId !== null && run?.status !== "COMPLETED" && !runError;
    const isRunning = createRun.isPending || isWaiting;
    const requestError = createRun.error ?? runError;

    function handleRun() {
        createRun.mutate({ dialect, sql }, { onSuccess: (created) => setRunId(created.id) });
    }

    return (
        <section aria-labelledby="query-run-title" className="rounded-lg border bg-card p-6">
            <h2 id="query-run-title" className="text-base font-bold">
                Chạy thử SQL
            </h2>
            <p className="mt-1 text-[13px] text-muted-foreground">
                Chạy câu lệnh trên mọi bộ dữ liệu của database đã lưu cho bài, kể cả bộ ẩn. Kết quả
                chỉ để bạn kiểm tra, chưa lưu thành đáp án.
            </p>
            {hasDataset ? null : (
                <p className="mt-3 text-[13px] text-muted-foreground">
                    Lưu database cho bài trước khi chạy thử.
                </p>
            )}
            <div className="mt-4 flex flex-col gap-1.5">
                <Label htmlFor="query-run-dialect">Dialect</Label>
                <NativeSelect
                    id="query-run-dialect"
                    className="w-48"
                    value={dialect}
                    onChange={(event) => setDialect(event.target.value as Dialect)}
                >
                    {DIALECTS.map((option) => (
                        <option key={option} value={option}>
                            {DIALECT_LABELS[option]}
                        </option>
                    ))}
                </NativeSelect>
            </div>
            <div className="mt-4 flex flex-col gap-1.5">
                <Label htmlFor="query-run-sql">Câu lệnh SQL</Label>
                <textarea
                    id="query-run-sql"
                    value={sql}
                    rows={6}
                    spellCheck={false}
                    placeholder="SELECT * FROM ..."
                    onChange={(event) => setSql(event.target.value)}
                    className="w-full resize-y rounded-md border bg-transparent p-3 text-[13px] outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
                />
            </div>
            <div className="mt-4 flex items-center gap-3">
                <Button
                    type="button"
                    disabled={!hasDataset || sql.trim() === "" || isRunning}
                    onClick={handleRun}
                >
                    Chạy thử
                </Button>
                <span role="status" className="text-[13px] text-muted-foreground">
                    {isRunning ? "Đang chạy…" : null}
                </span>
            </div>
            {requestError ? (
                <p role="alert" className="mt-3 text-[13px] text-destructive">
                    {requestError.message}
                </p>
            ) : null}
            {run?.status === "COMPLETED" ? (
                <div className="mt-5 border-t pt-4">
                    <RunResults run={run} />
                </div>
            ) : null}
        </section>
    );
}
