"use client";

import { CircleCheck } from "lucide-react";
import { useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { useToast } from "@/components/shared/ToastProvider";
import { ApiRequestError } from "@/lib/api-client";

import { useJudgeAttention, useJudgeQueue, useRejudgeSubmission } from "../../hooks/useAdminJudge";
import { StaleDataNotice } from "../StaleDataNotice";
import { SubmissionDetailDialog } from "../submissions/SubmissionDetailDialog";
import { BlockLoading } from "../users/BlockStates";
import { AttentionTable } from "./AttentionTable";
import { JudgeQueueTiles } from "./JudgeQueueTiles";

const QUEUE_TILE_COUNT = 5;

export function AdminJudge() {
    const showToast = useToast();
    const queue = useJudgeQueue();
    const attention = useJudgeAttention();
    const rejudge = useRejudgeSubmission();
    const [openedId, setOpenedId] = useState<string | null>(null);

    const handleRejudge = (submissionId: string) =>
        rejudge.mutate(submissionId, {
            onSuccess: () => showToast("Đã đưa bài nộp vào hàng đợi chấm lại"),
            onError: (error) => showToast(error.message, "error"),
        });

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-4 px-4 pt-6 pb-10 md:px-7">
            <h1 className="text-[26px] font-extrabold">Máy chấm</h1>
            {queue.isRefetchError || attention.isRefetchError ? (
                <StaleDataNotice
                    updatedAtMs={Math.min(queue.dataUpdatedAt, attention.dataUpdatedAt)}
                />
            ) : null}
            <section aria-label="Hàng đợi chấm">
                {queue.isPending ? (
                    <div
                        role="status"
                        aria-label="Đang tải hàng đợi"
                        className="grid grid-cols-[repeat(auto-fit,minmax(170px,1fr))] gap-3.5"
                    >
                        {Array.from({ length: QUEUE_TILE_COUNT }, (_, index) => (
                            <Skeleton key={index} className="h-24" />
                        ))}
                    </div>
                ) : null}
                {queue.data ? <JudgeQueueTiles queue={queue.data} /> : null}
                {!queue.data && queue.error ? (
                    <ErrorState
                        title="Chưa tải được hàng đợi"
                        message={queue.error.message}
                        traceId={
                            queue.error instanceof ApiRequestError ? queue.error.traceId : undefined
                        }
                        onRetry={() => void queue.refetch()}
                    />
                ) : null}
            </section>
            <section
                aria-labelledby="judge-attention"
                className="overflow-hidden rounded-lg border bg-card"
            >
                <div className="border-b px-4 py-3">
                    <h2 id="judge-attention" className="text-base font-bold">
                        Cần xử lý
                    </h2>
                    <p className="text-[13px] text-muted-foreground">
                        Bài bị kẹt trong hàng đợi và bài lỗi hệ thống chấm 7 ngày qua
                    </p>
                </div>
                {attention.isPending ? (
                    <div className="p-4">
                        <BlockLoading label="Đang tải bài cần xử lý" rows={4} />
                    </div>
                ) : null}
                {!attention.data && attention.error ? (
                    <ErrorState
                        title="Chưa tải được bài cần xử lý"
                        message={attention.error.message}
                        traceId={
                            attention.error instanceof ApiRequestError
                                ? attention.error.traceId
                                : undefined
                        }
                        onRetry={() => void attention.refetch()}
                        className="border-0"
                    />
                ) : null}
                {attention.data && attention.data.items.length > 0 ? (
                    <AttentionTable
                        submissions={attention.data.items}
                        rejudgingId={rejudge.isPending ? (rejudge.variables ?? null) : null}
                        onOpen={setOpenedId}
                        onRejudge={handleRejudge}
                    />
                ) : null}
                {attention.data && attention.data.items.length === 0 ? (
                    <EmptyState
                        title="Máy chấm đang ổn, không có bài nào cần xử lý"
                        icon={<CircleCheck />}
                        className="border-0"
                    />
                ) : null}
            </section>
            {openedId ? (
                <SubmissionDetailDialog submissionId={openedId} onClose={() => setOpenedId(null)} />
            ) : null}
        </main>
    );
}
