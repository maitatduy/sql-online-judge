"use client";

import { useSearchParams } from "next/navigation";

import { EmptyState } from "@/components/shared/EmptyState";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";

import {
    readReviewSelection,
    reviewHref,
    type ReviewSelection,
} from "../../problem-reviews/search-params";
import { ReviewProblemDetail } from "./ReviewProblemDetail";
import { ReviewQueue } from "./ReviewQueue";
import { ReviewStatusFilter } from "./ReviewStatusFilter";

function select(selection: ReviewSelection): void {
    window.history.replaceState(null, "", reviewHref(selection));
}

export function AdminProblemReviews() {
    const selection = readReviewSelection(useSearchParams());

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <div>
                <h1 className="text-[22px] font-extrabold tracking-[-0.02em]">Duyệt bài tập</h1>
                <p className="mt-1 text-[13px] text-muted-foreground">
                    Bài do người ra đề gửi lên cần được duyệt trước khi xuất bản.
                </p>
            </div>
            <ReviewStatusFilter
                status={selection.status}
                onChange={(status) => select({ status, problemId: null })}
            />
            <div className="grid items-start gap-4 lg:grid-cols-[340px_minmax(0,1fr)]">
                <ReviewQueue
                    status={selection.status}
                    selectedId={selection.problemId}
                    onSelect={(problemId) => select({ ...selection, problemId })}
                />
                {selection.problemId ? (
                    <ReviewProblemDetail
                        key={selection.problemId}
                        problemId={selection.problemId}
                    />
                ) : (
                    <section aria-label="Chi tiết bài" className="rounded-lg border bg-card">
                        <EmptyState
                            title="Chọn một bài trong hàng đợi để xem."
                            className="border-0"
                        />
                    </section>
                )}
            </div>
        </main>
    );
}
