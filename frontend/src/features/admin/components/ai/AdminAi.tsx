"use client";

import { ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useState } from "react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Button } from "@/components/ui/button";
import { ADMIN_ROUTES } from "@/lib/admin-navigation";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useNow } from "@/lib/useNow";
import { cn } from "@/lib/utils";

import type { AiBlockReason } from "../../ai/types";
import { useAiFlags, useAiSummary } from "../../hooks/useAdminAi";
import { StatTile } from "../StatTile";
import { AiTranscriptDialog } from "./AiTranscriptDialog";

const REASON_LABELS: Record<AiBlockReason, string> = {
    SOLUTION_REVEAL: "Lộ lời giải",
    MODEL_REFUSAL: "Mô hình từ chối",
};

const REASON_CLASSES: Record<AiBlockReason, string> = {
    SOLUTION_REVEAL: "bg-ai/12 text-ai",
    MODEL_REFUSAL: "bg-surface-2 text-muted-foreground",
};

function pageHref(page: number): string {
    return page > 1 ? `${ADMIN_ROUTES.ai}?page=${page}` : ADMIN_ROUTES.ai;
}

export function AdminAi() {
    const now = useNow();
    const page = Math.max(1, Number(useSearchParams().get("page")) || 1);
    const summary = useAiSummary();
    const flags = useAiFlags(page);
    const [openConversation, setOpenConversation] = useState<string | null>(null);

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-5 px-4 pt-6 pb-10 md:px-7">
            <h1 className="text-[24px] font-extrabold tracking-[-0.02em]">Trợ lý AI</h1>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                {summary.data ? (
                    <>
                        <StatTile
                            label="Câu hỏi hôm nay"
                            value={formatNumber(summary.data.questionsToday)}
                        >
                            {null}
                        </StatTile>
                        <StatTile
                            label="Bị chặn hôm nay"
                            value={formatNumber(summary.data.blockedToday)}
                        >
                            {null}
                        </StatTile>
                        <StatTile
                            label="Người hỏi hôm nay"
                            value={formatNumber(summary.data.activeUsersToday)}
                        >
                            {null}
                        </StatTile>
                        <StatTile
                            label="Lượt mỗi người mỗi ngày"
                            value={formatNumber(summary.data.dailyMessageLimit)}
                        >
                            <p className="mt-1 text-[13px] text-muted-foreground">
                                {summary.data.model}
                            </p>
                        </StatTile>
                    </>
                ) : (
                    Array.from({ length: 4 }, (_, index) => (
                        <Skeleton key={index} className="h-24" />
                    ))
                )}
            </div>
            <section aria-labelledby="ai-flags" className="flex flex-col gap-3">
                <h2 id="ai-flags" className="text-lg font-bold">
                    Câu trả lời bị chặn
                </h2>
                {flags.isPending ? <Skeleton className="h-48" /> : null}
                {!flags.data && flags.error ? (
                    <ErrorState
                        title="Chưa tải được danh sách"
                        message={flags.error.message}
                        traceId={
                            flags.error instanceof ApiRequestError ? flags.error.traceId : undefined
                        }
                        onRetry={() => void flags.refetch()}
                    />
                ) : null}
                {flags.data && flags.data.items.length === 0 ? (
                    <EmptyState title="Chưa có câu trả lời nào bị chặn." icon={<ShieldCheck />} />
                ) : null}
                {flags.data && flags.data.items.length > 0 ? (
                    <div className="overflow-x-auto rounded-lg border bg-card">
                        <table className="w-full text-[15px]">
                            <thead>
                                <tr className="border-b text-left text-sm text-muted-foreground">
                                    <th className="px-4 py-3 font-semibold">Thời gian</th>
                                    <th className="px-4 py-3 font-semibold">Người hỏi</th>
                                    <th className="px-4 py-3 font-semibold">Bài</th>
                                    <th className="px-4 py-3 font-semibold">Lý do</th>
                                    <th className="px-4 py-3 font-semibold">Câu hỏi</th>
                                    <th className="px-4 py-3" />
                                </tr>
                            </thead>
                            <tbody>
                                {flags.data.items.map((flag) => (
                                    <tr key={flag.messageId} className="border-b last:border-b-0">
                                        <td className="px-4 py-3 whitespace-nowrap text-muted-foreground">
                                            {formatRelativeTime(flag.createdAt, now)}
                                        </td>
                                        <td className="px-4 py-3 font-semibold">
                                            {flag.username ?? "Người dùng"}
                                        </td>
                                        <td className="px-4 py-3">
                                            {flag.problemSlug ? (
                                                <Link
                                                    href={ROUTES.problem(flag.problemSlug)}
                                                    className="text-brand hover:underline"
                                                >
                                                    {flag.problemSlug}
                                                </Link>
                                            ) : (
                                                "-"
                                            )}
                                        </td>
                                        <td className="px-4 py-3">
                                            <span
                                                className={cn(
                                                    "rounded-full px-2.5 py-0.5 text-sm font-semibold whitespace-nowrap",
                                                    REASON_CLASSES[flag.reason],
                                                )}
                                            >
                                                {REASON_LABELS[flag.reason]}
                                            </span>
                                        </td>
                                        <td className="max-w-md px-4 py-3">
                                            <span className="line-clamp-2">
                                                {flag.question ?? "-"}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3 text-right">
                                            <Button
                                                type="button"
                                                variant="outline"
                                                size="sm"
                                                onClick={() =>
                                                    setOpenConversation(flag.conversationId)
                                                }
                                            >
                                                Xem hội thoại
                                            </Button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : null}
                {flags.data && flags.data.totalPages > 1 ? (
                    <Pagination result={flags.data} hrefOf={pageHref} unit="câu trả lời" />
                ) : null}
            </section>
            {openConversation ? (
                <AiTranscriptDialog
                    conversationId={openConversation}
                    onClose={() => setOpenConversation(null)}
                />
            ) : null}
        </main>
    );
}
