"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";
import { useNow } from "@/lib/useNow";
import { cn } from "@/lib/utils";

import { useComments } from "../hooks/useCommunity";
import type { CommentSort, CommentThread } from "../types";
import { CommentForm } from "./CommentForm";
import { CommentView } from "./CommentView";

const SORT_LABELS: Record<CommentSort, string> = { TOP: "Hay nhất", NEW: "Mới nhất" };

interface CommentSectionProps {
    thread: CommentThread;
    title?: string;
    placeholder?: string;
}

export function CommentSection({
    thread,
    title,
    placeholder = "Viết bình luận… hỗ trợ Markdown",
}: CommentSectionProps) {
    const pathname = usePathname();
    const now = useNow();
    const [sort, setSort] = useState<CommentSort>("TOP");
    const { data: currentUser } = useCurrentUser();
    const { data, error, isPending, refetch } = useComments(thread, sort);
    const viewerId = currentUser?.id ?? null;

    return (
        <section aria-label="Bình luận" className="flex flex-col gap-4">
            <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-bold">
                    {title ?? `${formatNumber(data?.totalItems ?? 0)} bình luận`}
                </h2>
                <div
                    role="group"
                    aria-label="Sắp xếp bình luận"
                    className="flex rounded-md border p-0.5"
                >
                    {(Object.keys(SORT_LABELS) as CommentSort[]).map((option) => (
                        <button
                            key={option}
                            type="button"
                            aria-pressed={sort === option}
                            onClick={() => setSort(option)}
                            className={cn(
                                "rounded-sm px-3 py-1 text-sm font-semibold",
                                sort === option ? "bg-surface-2" : "text-muted-foreground",
                            )}
                        >
                            {SORT_LABELS[option]}
                        </button>
                    ))}
                </div>
            </div>
            {viewerId ? (
                <CommentForm thread={thread} placeholder={placeholder} />
            ) : (
                <p className="text-[15px] text-muted-foreground">
                    <Link
                        href={ROUTES.loginWithNext(pathname)}
                        className="font-semibold text-brand"
                    >
                        Đăng nhập
                    </Link>{" "}
                    để bình luận.
                </p>
            )}
            {isPending ? <Skeleton className="h-24" /> : null}
            {!data && error ? (
                <ErrorState
                    title="Chưa tải được bình luận"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {data ? (
                <ul className="flex flex-col gap-5">
                    {data.items.map((comment) => (
                        <CommentView
                            key={comment.id}
                            comment={comment}
                            thread={thread}
                            viewerId={viewerId}
                            now={now}
                        />
                    ))}
                </ul>
            ) : null}
        </section>
    );
}
