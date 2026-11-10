"use client";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { ErrorState } from "@/components/shared/ErrorState";
import { Markdown } from "@/components/shared/Markdown";
import { useToast } from "@/components/shared/ToastProvider";
import { ApiRequestError } from "@/lib/api-client";

import { useProblemAnswer } from "../hooks/useProblemAnswer";
import { useLatestAnswerCheck, useSubmitForReview } from "../hooks/usePublishStep";
import { publishConditionsOf } from "../publish-conditions";
import { EDITABLE_STATUSES, type StudioProblem } from "../types";
import { PublishConditions } from "./PublishConditions";
import { PublishStatusPanel } from "./PublishStatusPanel";

interface PublishStepProps {
    problem: StudioProblem;
}

export function PublishStep({ problem }: PublishStepProps) {
    const showToast = useToast();
    const hasDataset = problem.dataset !== null;
    const answer = useProblemAnswer(problem.id, hasDataset);
    const latestCheck = useLatestAnswerCheck(problem.id);
    const submit = useSubmitForReview(problem.id, () => showToast("Đã gửi bài cho admin duyệt."));
    const isEditable = EDITABLE_STATUSES.includes(problem.status);
    const conditions = publishConditionsOf(
        problem,
        hasDataset ? answer.data : null,
        latestCheck.data,
    );
    const loadError = answer.error ?? latestCheck.error;

    return (
        <div className="flex min-w-0 flex-col gap-6">
            <section
                aria-labelledby="publish-status-title"
                className="flex flex-col gap-4 rounded-lg border bg-card p-6"
            >
                <h2 id="publish-status-title" className="text-base font-bold">
                    Gửi duyệt
                </h2>
                {loadError ? (
                    <ErrorState
                        title="Không tải được điều kiện gửi duyệt"
                        message={loadError.message}
                        traceId={
                            loadError instanceof ApiRequestError ? loadError.traceId : undefined
                        }
                        onRetry={() => {
                            void answer.refetch();
                            void latestCheck.refetch();
                        }}
                    />
                ) : null}
                <PublishConditions
                    problemId={problem.id}
                    conditions={conditions}
                    canEdit={isEditable}
                />
                <div className="border-t pt-4">
                    <PublishStatusPanel
                        problem={problem}
                        canSubmit={conditions.every((condition) => condition.isMet)}
                        isSubmitting={submit.isPending}
                        submitError={submit.error?.message ?? null}
                        onSubmit={() => submit.mutate()}
                    />
                </div>
            </section>
            <section
                aria-label="Xem trước đề bài"
                className="flex flex-col gap-3 rounded-lg border bg-card p-6"
            >
                <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                    Xem trước như người học thấy
                </p>
                <div className="flex flex-wrap items-center gap-3">
                    <h2 className="text-[22px] font-extrabold tracking-[-0.02em]">
                        {problem.title}
                    </h2>
                    <DifficultyLabel difficulty={problem.difficulty} />
                </div>
                {problem.statementMarkdown.trim() ? (
                    <Markdown>{problem.statementMarkdown}</Markdown>
                ) : (
                    <p className="text-[13px] text-muted-foreground">Chưa có đề bài.</p>
                )}
            </section>
        </div>
    );
}
