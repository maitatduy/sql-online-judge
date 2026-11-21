"use client";

import { useState } from "react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import type { CourseReviewAction } from "../../course-reviews/types";
import { useCourseReviewDecision } from "../../hooks/useCourseReviews";

const REASON_ID = "course-review-reason";
const MAX_REASON_LENGTH = 2000;

interface CourseDecisionFormProps {
    courseId: string;
    title: string;
    onReviewed: () => void;
}

function toastOf(action: CourseReviewAction, title: string): string {
    return action === "approve"
        ? `Đã xuất bản khoá ${title}.`
        : `Đã trả khoá ${title} về cho người soạn.`;
}

export function CourseDecisionForm({ courseId, title, onReviewed }: CourseDecisionFormProps) {
    const showToast = useToast();
    const [reason, setReason] = useState("");
    const [reasonError, setReasonError] = useState<string | null>(null);
    const decision = useCourseReviewDecision(courseId, (_, action) => {
        showToast(toastOf(action, title));
        onReviewed();
    });

    function decide(action: CourseReviewAction) {
        const trimmed = reason.trim();
        if (action === "request-changes" && trimmed === "") {
            setReasonError("Cần ghi lý do");
            return;
        }
        setReasonError(null);
        decision.mutate({ action, reason: action === "approve" ? null : trimmed });
    }

    return (
        <div className="flex flex-col gap-3 border-t pt-4">
            <div className="flex flex-col gap-1.5">
                <label htmlFor={REASON_ID} className="text-sm font-semibold">
                    Lý do gửi người soạn
                </label>
                <textarea
                    id={REASON_ID}
                    rows={3}
                    value={reason}
                    maxLength={MAX_REASON_LENGTH}
                    aria-invalid={reasonError ? true : undefined}
                    aria-describedby={reasonError ? `${REASON_ID}-error` : `${REASON_ID}-hint`}
                    onChange={(event) => setReason(event.target.value)}
                    className="rounded-md border border-input bg-transparent px-3 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
                />
                {reasonError ? (
                    <p id={`${REASON_ID}-error`} className="text-sm text-destructive">
                        {reasonError}
                    </p>
                ) : (
                    <p id={`${REASON_ID}-hint`} className="text-sm text-muted-foreground">
                        Bắt buộc khi yêu cầu sửa.
                    </p>
                )}
            </div>
            {decision.error ? (
                <p role="alert" className="text-sm text-destructive">
                    {decision.error.message}
                </p>
            ) : null}
            <div className="flex flex-wrap justify-end gap-2">
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
                    disabled={decision.isPending}
                    onClick={() => decide("approve")}
                >
                    Duyệt và xuất bản
                </Button>
            </div>
        </div>
    );
}
