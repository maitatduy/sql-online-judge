"use client";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { DIALECT_LABELS } from "@/lib/dialect";

import { useSubmissionCode } from "../hooks/useSubmissionCode";

const SKELETON_LINE_COUNT = 3;

interface SubmissionCodeViewProps {
    submissionId: string;
}

export function SubmissionCodeView({ submissionId }: SubmissionCodeViewProps) {
    const { data, error, isPending, refetch } = useSubmissionCode(submissionId);

    if (isPending) {
        return (
            <div role="status" aria-label="Đang tải mã bài nộp" className="flex flex-col gap-2">
                {Array.from({ length: SKELETON_LINE_COUNT }, (_, index) => (
                    <Skeleton key={index} className="h-3.5" />
                ))}
            </div>
        );
    }

    if (error) {
        return (
            <ErrorState
                title="Không tải được mã bài nộp"
                message={error.message}
                traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                onRetry={() => void refetch()}
            />
        );
    }

    return (
        <figure className="flex flex-col gap-2">
            <figcaption className="text-xs text-muted-foreground">
                {`Mã đã nộp, ${DIALECT_LABELS[data.dialect]}`}
            </figcaption>
            <pre className="max-h-80 overflow-auto rounded-lg border bg-background px-4 py-3 font-mono text-[13px] whitespace-pre">
                {data.code}
            </pre>
        </figure>
    );
}
