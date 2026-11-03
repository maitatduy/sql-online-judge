"use client";

import Link from "next/link";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import type { CompletionSchema } from "@/lib/sql-completion";

import { useProblemAnswer } from "../hooks/useProblemAnswer";
import { useCompletionSchema } from "../hooks/useStudioRun";
import type { StudioProblem } from "../types";
import { AnswerForm } from "./AnswerForm";

const EMPTY_SCHEMA: CompletionSchema = { tables: [] };

interface AnswerStepProps {
    problem: StudioProblem;
    disabled: boolean;
}

export function AnswerStep({ problem, disabled }: AnswerStepProps) {
    const hasDataset = problem.dataset !== null;
    const { data: answer, error, isPending, refetch } = useProblemAnswer(problem.id, hasDataset);
    const { data: schema } = useCompletionSchema(problem.dataset?.id ?? null);

    return (
        <section aria-labelledby="answer-step-title" className="rounded-lg border bg-card p-6">
            <h2 id="answer-step-title" className="text-base font-bold">
                Đáp án và testcase
            </h2>
            <p className="mt-1 text-[13px] text-muted-foreground">
                Đáp án chuẩn dùng để tính kết quả mong đợi của từng testcase khi kiểm tra bài.
            </p>
            <div className="mt-5">
                {!hasDataset ? (
                    <p className="text-[13px] text-muted-foreground">
                        Chọn và lưu database cho bài ở bước 3 trước.{" "}
                        <Link
                            href={`/studio/problems/${problem.id}/database`}
                            className="font-semibold text-brand hover:underline"
                        >
                            Sang bước Database
                        </Link>
                    </p>
                ) : null}
                {hasDataset && isPending ? (
                    <div role="status" aria-label="Đang tải đáp án" className="flex flex-col gap-3">
                        <Skeleton className="h-6 w-48" />
                        <Skeleton className="h-48" />
                    </div>
                ) : null}
                {error ? (
                    <ErrorState
                        title="Không tải được đáp án"
                        message={error.message}
                        traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                        onRetry={() => void refetch()}
                    />
                ) : null}
                {answer ? (
                    <AnswerForm
                        problemId={problem.id}
                        answer={answer}
                        schema={schema ?? EMPTY_SCHEMA}
                        disabled={disabled}
                    />
                ) : null}
            </div>
        </section>
    );
}
