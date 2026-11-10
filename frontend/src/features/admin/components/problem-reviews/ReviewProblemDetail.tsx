"use client";

import Link from "next/link";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { ErrorState } from "@/components/shared/ErrorState";
import { Markdown } from "@/components/shared/Markdown";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatDate } from "@/lib/format";

import { useReviewAnswerCheck, useReviewProblem } from "../../hooks/useProblemReviews";
import {
    answerCheckSummaryOf,
    REVIEW_DECISION_LABELS,
    REVIEW_STATUS_LABELS,
} from "../../problem-reviews/labels";
import { ReviewDecisionForm } from "./ReviewDecisionForm";

interface ReviewProblemDetailProps {
    problemId: string;
}

export function ReviewProblemDetail({ problemId }: ReviewProblemDetailProps) {
    const { data: problem, error, isPending, refetch } = useReviewProblem(problemId);
    const check = useReviewAnswerCheck(problemId);
    const summary = answerCheckSummaryOf(check.data);

    return (
        <section
            aria-label="Chi tiết bài"
            className="flex min-w-0 flex-col gap-4 rounded-lg border bg-card p-5"
        >
            {isPending ? (
                <div role="status" aria-label="Đang tải bài" className="flex flex-col gap-3">
                    <Skeleton className="h-7 w-72" />
                    <Skeleton className="h-40" />
                </div>
            ) : null}
            {error ? (
                <ErrorState
                    title="Chưa tải được bài"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="border-0"
                />
            ) : null}
            {problem ? (
                <>
                    <div className="flex flex-wrap items-start justify-between gap-3">
                        <div className="flex min-w-0 flex-col gap-1.5">
                            <h2 className="text-xl font-extrabold">{problem.title}</h2>
                            <p className="flex flex-wrap items-center gap-2 text-[13px] text-muted-foreground">
                                <DifficultyLabel difficulty={problem.difficulty} />
                                <span>{REVIEW_STATUS_LABELS[problem.status]}</span>
                                {problem.dataset ? (
                                    <span>Database: {problem.dataset.name}</span>
                                ) : null}
                                {problem.tags.length > 0 ? (
                                    <span>
                                        Chủ đề: {problem.tags.map((tag) => tag.name).join(", ")}
                                    </span>
                                ) : null}
                            </p>
                        </div>
                        <Link
                            href={`/studio/problems/${problem.id}/publish`}
                            className="text-[13px] font-semibold text-brand hover:underline"
                        >
                            Mở trong Studio
                        </Link>
                    </div>
                    {problem.latestReview ? (
                        <div className="rounded-md bg-surface-2 px-3 py-2 text-[13px]">
                            <p className="font-semibold">
                                {REVIEW_DECISION_LABELS[problem.latestReview.decision]} ngày{" "}
                                {formatDate(problem.latestReview.reviewedAt)}
                            </p>
                            {problem.latestReview.reason ? (
                                <p className="mt-1 whitespace-pre-line">
                                    {problem.latestReview.reason}
                                </p>
                            ) : null}
                        </div>
                    ) : null}
                    <div className="flex flex-col gap-1">
                        <h3 className="text-sm font-semibold">Kiểm tra đáp án</h3>
                        <p className="text-[13px]">{summary.text}</p>
                    </div>
                    <div className="flex flex-col gap-2">
                        <h3 className="text-sm font-semibold">Đề bài</h3>
                        {problem.statementMarkdown.trim() ? (
                            <div className="rounded-md border p-4">
                                <Markdown>{problem.statementMarkdown}</Markdown>
                            </div>
                        ) : (
                            <p className="text-[13px] text-muted-foreground">Chưa có đề bài.</p>
                        )}
                    </div>
                    {problem.status === "PENDING_REVIEW" ? (
                        <ReviewDecisionForm
                            problemId={problem.id}
                            title={problem.title}
                            canApprove={summary.canApprove}
                        />
                    ) : null}
                </>
            ) : null}
        </section>
    );
}
