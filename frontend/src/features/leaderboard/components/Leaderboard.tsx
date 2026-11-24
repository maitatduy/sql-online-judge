"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Trophy } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";

import { Avatar } from "@/components/shared/Avatar";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import { leaderboardApi } from "../api";

const PODIUM_SIZE = 3;

function pageHref(page: number): string {
    return page > 1 ? `${ROUTES.leaderboard}?page=${page}` : ROUTES.leaderboard;
}

export function Leaderboard() {
    const page = Math.max(1, Number(useSearchParams().get("page")) || 1);
    const { data, error, isPending, refetch } = useQuery({
        queryKey: ["leaderboard", page],
        queryFn: ({ signal }) => leaderboardApi.page(page, signal),
        placeholderData: keepPreviousData,
    });

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-4xl flex-col gap-5 px-4 pt-6 pb-16 md:px-6"
        >
            <div>
                <h1 className="text-[28px] font-extrabold">Bảng xếp hạng</h1>
                <p className="mt-1 text-[15px] text-muted-foreground">
                    Rating sau các contest tính rating.
                </p>
            </div>
            {isPending ? <Skeleton className="h-96" /> : null}
            {!data && error ? (
                <ErrorState
                    title="Chưa tải được bảng xếp hạng"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {data && data.items.length === 0 ? (
                <EmptyState title="Chưa có ai thi contest tính rating." icon={<Trophy />} />
            ) : null}
            {data && data.items.length > 0 ? (
                <div className="overflow-x-auto rounded-lg border bg-card">
                    <table className="w-full text-[15px]">
                        <thead>
                            <tr className="border-b text-left text-sm text-muted-foreground">
                                <th className="w-20 px-4 py-3 font-semibold">Hạng</th>
                                <th className="px-4 py-3 font-semibold">Người thi</th>
                                <th className="px-4 py-3 text-right font-semibold">Rating</th>
                                <th className="px-4 py-3 text-right font-semibold">Contest</th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.items.map((entry) => {
                                const name = entry.username ?? "Người dùng";
                                return (
                                    <tr key={entry.userId} className="border-b last:border-b-0">
                                        <td
                                            className={cn(
                                                "px-4 py-3 font-bold",
                                                entry.rank <= PODIUM_SIZE
                                                    ? "text-lg text-brand"
                                                    : "text-muted-foreground",
                                            )}
                                        >
                                            {formatNumber(entry.rank)}
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className="flex items-center gap-2.5">
                                                <Avatar name={name} />
                                                {entry.username ? (
                                                    <Link
                                                        href={ROUTES.profile(entry.username)}
                                                        className="font-semibold hover:text-brand"
                                                    >
                                                        {entry.displayName ?? entry.username}
                                                    </Link>
                                                ) : (
                                                    <span className="font-semibold">{name}</span>
                                                )}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3 text-right text-lg font-extrabold">
                                            {formatNumber(entry.rating)}
                                        </td>
                                        <td className="px-4 py-3 text-right text-muted-foreground">
                                            {formatNumber(entry.contestsPlayed)}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            ) : null}
            {data && data.totalPages > 1 ? (
                <Pagination result={data} hrefOf={pageHref} unit="người" />
            ) : null}
        </main>
    );
}
