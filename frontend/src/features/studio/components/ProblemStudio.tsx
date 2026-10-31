"use client";

import Link from "next/link";

import { ArrowLeft } from "lucide-react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { useToast } from "@/components/shared/ToastProvider";
import { ApiRequestError } from "@/lib/api-client";
import { formatTime } from "@/lib/format";

import { useProblemAutosave } from "../hooks/useProblemAutosave";
import { useStudioProblem } from "../hooks/useStudioProblem";
import { LOCKED_MESSAGES, PROBLEM_STATUS_LABELS } from "../status-labels";
import { EDITABLE_STATUSES, type StudioStep } from "../types";
import { InfoStep } from "./InfoStep";
import { StatementStep } from "./StatementStep";
import { StudioSteps } from "./StudioSteps";

const FIELD_ERROR_CODES = ["SLUG_TAKEN", "VALIDATION_FAILED"];

interface ProblemStudioProps {
    problemId: string;
    step?: StudioStep;
}

function isFieldError(error: Error): boolean {
    return error instanceof ApiRequestError && FIELD_ERROR_CODES.includes(error.code);
}

export function ProblemStudio({ problemId, step = "info" }: ProblemStudioProps) {
    const showToast = useToast();
    const { data: problem, error, isPending, refetch } = useStudioProblem(problemId);
    const autosave = useProblemAutosave(problemId, (saveError) => {
        if (!isFieldError(saveError)) {
            showToast(`Chưa lưu được thay đổi. ${saveError.message}`, "error");
        }
    });

    const isEditable = problem !== undefined && EDITABLE_STATUSES.includes(problem.status);

    return (
        <div className="mx-auto flex max-w-[1280px] flex-col gap-5 px-6 pt-7 pb-16">
            <Link
                href="/studio/problems"
                className="inline-flex w-fit items-center gap-1.5 text-[13px] font-semibold text-muted-foreground hover:text-foreground"
            >
                <ArrowLeft aria-hidden="true" className="size-4" />
                Quay lại Bài của tôi
            </Link>
            {isPending ? (
                <div role="status" aria-label="Đang tải bài" className="flex flex-col gap-3">
                    <Skeleton className="h-9 w-80" />
                    <Skeleton className="h-64" />
                </div>
            ) : null}
            {error && error instanceof ApiRequestError && error.status === 404 ? (
                <p className="rounded-lg border bg-card p-6 text-center font-bold">
                    Không tìm thấy bài này.
                </p>
            ) : null}
            {error && !(error instanceof ApiRequestError && error.status === 404) ? (
                <ErrorState
                    title="Không tải được bài"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {problem ? (
                <>
                    <div className="flex flex-wrap items-end justify-between gap-3">
                        <div>
                            <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                Problem Studio
                            </p>
                            <h1 className="mt-1.5 text-[22px] font-extrabold tracking-[-0.02em]">
                                {problem.title}
                            </h1>
                        </div>
                        <div className="flex items-center gap-3 text-[13px] text-muted-foreground">
                            <span className="rounded-full bg-surface-2 px-2.5 py-0.5 text-xs font-semibold text-foreground">
                                {PROBLEM_STATUS_LABELS[problem.status]}
                            </span>
                            <span role="status">
                                {autosave.isSaving ? "Đang lưu…" : null}
                                {!autosave.isSaving && autosave.savedAt
                                    ? `Bản nháp · lưu ${formatTime(autosave.savedAt)}`
                                    : null}
                            </span>
                        </div>
                    </div>
                    {isEditable ? null : (
                        <p className="rounded-md bg-surface-2 px-3 py-2 text-[13px]">
                            {LOCKED_MESSAGES[problem.status]}
                        </p>
                    )}
                    <div className="grid gap-6 md:grid-cols-[220px_1fr]">
                        <StudioSteps problemId={problem.id} currentStep={step} />
                        {step === "info" ? (
                            <InfoStep
                                key={problem.id}
                                problem={problem}
                                disabled={!isEditable}
                                saveError={autosave.error}
                                onChange={autosave.queueSave}
                            />
                        ) : (
                            <StatementStep
                                key={problem.id}
                                problem={problem}
                                disabled={!isEditable}
                                saveError={autosave.error}
                                onChange={autosave.queueSave}
                            />
                        )}
                    </div>
                </>
            ) : null}
        </div>
    );
}
