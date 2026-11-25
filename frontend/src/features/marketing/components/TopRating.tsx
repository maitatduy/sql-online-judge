"use client";

import Link from "next/link";

import { Avatar } from "@/components/shared/Avatar";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import { useTopRated } from "../hooks/useLanding";

const SKELETON_COUNT = 5;

export function TopRating() {
    const { data, error, isPending, refetch } = useTopRated();
    const users = data?.items;

    return (
        <section aria-labelledby="top-rating" className="min-w-0 flex-[1_1_320px]">
            <div className="mb-5 flex items-baseline justify-between">
                <h2 id="top-rating" className="text-[28px] font-extrabold">
                    Top rating
                </h2>
                <Link
                    href={ROUTES.leaderboard}
                    className="inline-flex min-h-11 items-center text-brand underline-offset-4 hover:underline md:min-h-0"
                >
                    Bảng xếp hạng
                </Link>
            </div>
            {isPending ? (
                <div role="status" aria-label="Đang tải top rating" className="grid gap-2">
                    {Array.from({ length: SKELETON_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-11" />
                    ))}
                </div>
            ) : null}
            {!users && error ? (
                <ErrorState
                    title="Chưa tải được top rating"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {users && users.length === 0 ? (
                <p className="rounded-lg border bg-card p-5 text-muted-foreground">
                    Chưa ai có rating. Thi một contest để lên bảng đầu tiên.
                </p>
            ) : null}
            {users && users.length > 0 ? (
                <ol className="overflow-hidden rounded-lg border bg-card">
                    {users.map((user, index) => {
                        const name = user.username ?? "Người dùng";
                        return (
                            <li
                                key={user.userId}
                                className="flex items-center gap-3 border-b px-4 py-2.5 last:border-b-0"
                            >
                                <span
                                    className={cn(
                                        "w-5 font-extrabold",
                                        index > 0 && "text-muted-foreground",
                                    )}
                                >
                                    {index + 1}
                                </span>
                                <Avatar name={name} className="size-8 text-xs" />
                                {user.username ? (
                                    <Link
                                        href={ROUTES.profile(user.username)}
                                        className="min-w-0 flex-1 truncate font-semibold hover:text-brand"
                                    >
                                        {user.username}
                                    </Link>
                                ) : (
                                    <span className="min-w-0 flex-1 truncate">{name}</span>
                                )}
                                <span className="font-extrabold">{formatNumber(user.rating)}</span>
                            </li>
                        );
                    })}
                </ol>
            ) : null}
        </section>
    );
}
