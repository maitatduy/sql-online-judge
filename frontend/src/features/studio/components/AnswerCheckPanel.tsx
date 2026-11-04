"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

import { useAnswerCheck, useStartAnswerCheck } from "../hooks/useAnswerCheck";
import type { AnswerCheck } from "../types";
import { AnswerCheckTable } from "./AnswerCheckTable";

const PASSED_MESSAGE = "Đạt: đáp án chuẩn cho cùng kết quả trên mọi dialect.";
const FAILED_MESSAGE = "Chưa đạt: sửa đáp án ở các testcase được đánh dấu rồi kiểm tra lại.";

interface AnswerCheckPanelProps {
    problemId: string;
    canCheck: boolean;
}

function progressOf(check: AnswerCheck): string {
    return `Đang kiểm tra: ${check.checkedCount}/${check.testcaseCount} testcase`;
}

export function AnswerCheckPanel({ problemId, canCheck }: AnswerCheckPanelProps) {
    const [started, setStarted] = useState<AnswerCheck | null>(null);
    const start = useStartAnswerCheck(problemId);
    const { data: fetched, error: fetchError } = useAnswerCheck(problemId, started?.id ?? null);
    const check = fetched ?? started;
    const isChecking = start.isPending || (check?.status === "PENDING" && !fetchError);
    const requestError = start.error ?? fetchError;

    function handleStart() {
        start.mutate(undefined, { onSuccess: setStarted });
    }

    return (
        <section aria-labelledby="answer-check-title" className="flex flex-col gap-3 border-t pt-4">
            <div>
                <h3 id="answer-check-title" className="text-sm font-semibold">
                    Kiểm tra đáp án
                </h3>
                <p className="mt-1 text-[13px] text-muted-foreground">
                    Chạy đáp án chuẩn trên mọi testcase và mọi dialect, so với kết quả PostgreSQL.
                </p>
            </div>
            <div className="flex flex-wrap items-center gap-3">
                <Button
                    type="button"
                    variant="outline"
                    disabled={!canCheck || isChecking}
                    onClick={handleStart}
                >
                    Kiểm tra đáp án
                </Button>
                <span role="status" className="text-[13px] text-muted-foreground">
                    {canCheck ? null : "Lưu đáp án và testcase trước khi kiểm tra."}
                    {canCheck && isChecking && check ? progressOf(check) : null}
                </span>
            </div>
            {requestError ? (
                <p role="alert" className="text-[13px] text-destructive">
                    {requestError.message}
                </p>
            ) : null}
            {check && check.status !== "PENDING" ? (
                <>
                    <p
                        className={cn(
                            "text-[13px] font-semibold",
                            check.status === "PASSED" ? "text-foreground" : "text-destructive",
                        )}
                    >
                        {check.status === "PASSED" ? PASSED_MESSAGE : FAILED_MESSAGE}
                    </p>
                    <AnswerCheckTable testcases={check.testcases} />
                </>
            ) : null}
        </section>
    );
}
