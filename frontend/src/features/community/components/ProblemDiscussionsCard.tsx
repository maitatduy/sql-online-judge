"use client";

import { MessagesSquare } from "lucide-react";
import Link from "next/link";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useNow } from "@/lib/useNow";

import { useProblemDiscussions } from "../hooks/useCommunity";

const SKELETON_COUNT = 3;

export function ProblemDiscussionsCard() {
    const now = useNow();
    const { data: discussions, error, isPending, refetch } = useProblemDiscussions();

    return (
        <section
            aria-labelledby="problem-discussions"
            className="flex flex-col gap-3 rounded-lg border bg-card p-5"
        >
            <h2 id="problem-discussions" className="text-base font-bold">
                Thảo luận mới ở các bài
            </h2>
            {isPending ? (
                <div role="status" aria-label="Đang tải thảo luận" className="grid gap-3">
                    {Array.from({ length: SKELETON_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-12" />
                    ))}
                </div>
            ) : null}
            {!discussions && error ? (
                <ErrorState
                    title="Chưa tải được thảo luận"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="border-0 p-0"
                />
            ) : null}
            {discussions && discussions.length === 0 ? (
                <p className="flex items-center gap-2 text-sm text-muted-foreground">
                    <MessagesSquare aria-hidden="true" className="size-4" />
                    Chưa có thảo luận nào ở các bài.
                </p>
            ) : null}
            {discussions && discussions.length > 0 ? (
                <ul className="flex flex-col divide-y">
                    {discussions.map((discussion) => (
                        <li key={discussion.commentId} className="py-2.5 first:pt-0 last:pb-0">
                            <Link
                                href={ROUTES.problemDiscussion(discussion.problemSlug)}
                                className="block rounded-md hover:text-brand"
                            >
                                <span className="block font-semibold">
                                    {discussion.problemTitle}
                                </span>
                                <span className="line-clamp-2 text-sm text-muted-foreground">
                                    {discussion.excerpt}
                                </span>
                            </Link>
                            <p className="mt-1 text-[13px] text-muted-foreground">
                                {`${discussion.authorUsername ?? "Người dùng"}${discussion.isReply ? " trả lời" : ""} - ${formatRelativeTime(discussion.createdAt, now)}`}
                            </p>
                        </li>
                    ))}
                </ul>
            ) : null}
        </section>
    );
}
