"use client";

import { useState } from "react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import { useReviewDecision } from "../../hooks/useProblemReviews";
import type { ReviewAction } from "../../problem-reviews/types";

const REASON_ID = "review-reason";
const REASON_REQUIRED = "Cần ghi lý do";

interface ReviewDecisionFormProps {
    problemId: string;
    title: string;
    canApprove: boolean;
}

function toastOf(action: ReviewAction, title: string): string {
    if (action === "approve") {
        return `Đã xuất bản bài ${title}.`;
    }
    return action === "reject"
        ? `Đã từ chối bài ${title}.`
        : `Đã trả bài ${title} về cho người ra đề.`;
}

export function ReviewDecisionForm({ problemId, title, canApprove }: ReviewDecisionFormProps) {
    const showToast = useToast();
    const [reason, setReason] = useState("");
    const [reasonError, setReasonError] = useState<string | null>(null);
    const decision = useReviewDecision(problemId, (_, action) => showToast(toastOf(action, title)));

    function decide(action: ReviewAction) {
        const trimmed = reason.trim();
        if (action !== "approve" && trimmed === "") {
            setReasonError(REASON_REQUIRED);
            return;
        }
        setReasonError(null);
        decision.mutate({ action, reason: action === "approve" ? null : trimmed });
    }

    return (
        <div className="flex flex-col gap-3 border-t pt-4">
            <div className="flex flex-col gap-1.5">
                <label htmlFor={REASON_ID} className="text-sm font-semibold">
                    Lý do gửi người ra đề
                </label>
                <textarea
                    id={REASON_ID}
                    rows={3}
                    value={reason}
                    maxLength={2000}
                    aria-invalid={reasonError ? true : undefined}
                    aria-describedby={reasonError ? `${REASON_ID}-error` : `${REASON_ID}-hint`}
                    onChange={(event) => setReason(event.target.value)}
                    className="rounded-md border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
                />
                {reasonError ? (
                    <p id={`${REASON_ID}-error`} className="text-[13px] text-destructive">
                        {reasonError}
                    </p>
                ) : (
                    <p id={`${REASON_ID}-hint`} className="text-[13px] text-muted-foreground">
                        Bắt buộc khi yêu cầu sửa hoặc từ chối.
                    </p>
                )}
            </div>
            {decision.error ? (
                <p role="alert" className="text-[13px] text-destructive">
                    {decision.error.message}
                </p>
            ) : null}
            <div className="flex flex-wrap gap-2">
                <Button
                    type="button"
                    variant="outline"
                    className="text-destructive"
                    disabled={decision.isPending}
                    onClick={() => decide("reject")}
                >
                    Từ chối
                </Button>
                <Button
                    type="button"
                    variant="outline"
                    disabled={decision.isPending}
                    onClick={() => decide("request-changes")}
                >
                    Yêu cầu sửa
                </Button>
                <Button
                    type="button"
                    disabled={!canApprove || decision.isPending}
                    onClick={() => decide("approve")}
                >
                    Duyệt và xuất bản
                </Button>
            </div>
            {canApprove ? null : (
                <p className="text-[13px] text-muted-foreground">
                    Chỉ duyệt được khi lần kiểm tra đáp án gần nhất đạt.
                </p>
            )}
        </div>
    );
}
