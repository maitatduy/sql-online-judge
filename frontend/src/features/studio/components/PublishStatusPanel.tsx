import Link from "next/link";

import { Button } from "@/components/ui/button";

import type { StudioProblem } from "../types";

interface PublishStatusPanelProps {
    problem: StudioProblem;
    canSubmit: boolean;
    isSubmitting: boolean;
    submitError: string | null;
    onSubmit: () => void;
}

function ReviewReason({ title, reason }: { title: string; reason: string | null | undefined }) {
    return (
        <div className="rounded-md bg-surface-2 px-3 py-2 text-[13px]">
            <p className="font-semibold">{title}</p>
            {reason ? <p className="mt-1 whitespace-pre-line">{reason}</p> : null}
        </div>
    );
}

export function PublishStatusPanel({
    problem,
    canSubmit,
    isSubmitting,
    submitError,
    onSubmit,
}: PublishStatusPanelProps) {
    const reason = problem.latestReview?.reason;

    if (problem.status === "PENDING_REVIEW") {
        return <p className="text-[13px]">Bài đang chờ admin duyệt.</p>;
    }
    if (problem.status === "PUBLISHED") {
        return (
            <div className="flex flex-wrap items-center gap-3 text-[13px]">
                <p>Bài đã xuất bản với số {problem.number}.</p>
                <Link
                    href={`/problems/${problem.slug}`}
                    className="font-semibold text-brand hover:underline"
                >
                    Xem trang bài
                </Link>
            </div>
        );
    }
    if (problem.status === "REJECTED") {
        return <ReviewReason title="Admin đã từ chối bài này" reason={reason} />;
    }
    return (
        <div className="flex flex-col gap-3">
            {problem.status === "CHANGES_REQUESTED" ? (
                <ReviewReason title="Admin yêu cầu sửa" reason={reason} />
            ) : null}
            <p className="text-[13px] text-muted-foreground">
                Sau khi gửi, bài bị khoá sửa cho tới khi admin duyệt xong.
            </p>
            {submitError ? (
                <p role="alert" className="text-[13px] text-destructive">
                    {submitError}
                </p>
            ) : null}
            <Button
                type="button"
                className="w-fit"
                disabled={!canSubmit || isSubmitting}
                onClick={onSubmit}
            >
                Gửi duyệt
            </Button>
        </div>
    );
}
