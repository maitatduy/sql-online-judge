"use client";

import { useState } from "react";

import { Markdown } from "@/components/shared/Markdown";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";

import { MAX_STATEMENT_LENGTH } from "../schemas";
import type { StudioProblem, UpdateProblemRequest } from "../types";

const STATEMENT_FIELD = "statementMarkdown";
const TOO_LONG_MESSAGE = "Đề bài tối đa 50 KB";

interface StatementStepProps {
    problem: StudioProblem;
    disabled: boolean;
    saveError: Error | null;
    onChange: (change: UpdateProblemRequest) => void;
}

function serverErrorOf(error: Error | null): string | undefined {
    return error instanceof ApiRequestError
        ? error.errors.find((fieldError) => fieldError.field === STATEMENT_FIELD)?.message
        : undefined;
}

export function StatementStep({ problem, disabled, saveError, onChange }: StatementStepProps) {
    const [statement, setStatement] = useState(problem.statementMarkdown);
    const isTooLong = statement.length > MAX_STATEMENT_LENGTH;
    const error = isTooLong ? TOO_LONG_MESSAGE : serverErrorOf(saveError);

    function changeStatement(value: string) {
        setStatement(value);
        if (value.length <= MAX_STATEMENT_LENGTH) {
            onChange({ statementMarkdown: value });
        }
    }

    return (
        <section aria-labelledby="statement-step-title" className="rounded-lg border bg-card p-6">
            <h2 id="statement-step-title" className="text-base font-bold">
                Đề bài
            </h2>
            <p className="mt-1 text-[13px] text-muted-foreground">
                Viết bằng Markdown: mô tả bảng, yêu cầu và ví dụ kết quả.
            </p>
            <Tabs defaultValue="write" className="mt-4 overflow-hidden rounded-md border">
                <TabsList>
                    <TabsTrigger value="write">Soạn</TabsTrigger>
                    <TabsTrigger value="preview">Xem trước</TabsTrigger>
                </TabsList>
                <TabsContent value="write" forceMount className="data-[state=inactive]:hidden">
                    <textarea
                        aria-label="Đề bài"
                        value={statement}
                        disabled={disabled}
                        spellCheck={false}
                        aria-invalid={error ? true : undefined}
                        aria-describedby={error ? "statement-error" : "statement-length"}
                        onChange={(event) => changeStatement(event.target.value)}
                        className="block min-h-[420px] w-full resize-y bg-transparent p-4 font-mono text-[13px] leading-relaxed outline-none disabled:cursor-not-allowed disabled:opacity-60"
                    />
                </TabsContent>
                <TabsContent value="preview" className="min-h-[420px] p-4">
                    {statement.trim() ? (
                        <Markdown>{statement}</Markdown>
                    ) : (
                        <p className="text-muted-foreground">Chưa có nội dung để xem trước.</p>
                    )}
                </TabsContent>
            </Tabs>
            <div className="mt-2 flex justify-between gap-3 text-[13px]">
                {error ? (
                    <p id="statement-error" className="text-destructive">
                        {error}
                    </p>
                ) : (
                    <span />
                )}
                <p id="statement-length" className="text-muted-foreground">
                    {formatNumber(statement.length)} / {formatNumber(MAX_STATEMENT_LENGTH)} ký tự
                </p>
            </div>
        </section>
    );
}
