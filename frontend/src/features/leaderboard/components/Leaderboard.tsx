"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Trophy } from "lucide-react";
import { useSearchParams } from "next/navigation";

import { Breadcrumb } from "@/components/shared/Breadcrumb";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { leaderboardApi } from "../api";
import { LeaderboardTable } from "./LeaderboardTable";

function pageHref(page: number): string {
    return page > 1 ? `${ROUTES.leaderboard}?page=${page}` : ROUTES.leaderboard;
}

export function Leaderboard() {
    const page = Math.max(1, Number(useSearchParams().get("page")) || 1);
    const { data: currentUser } = useCurrentUser();
    const { data, error, isPending, refetch } = useQuery({
        queryKey: ["leaderboard", page],
        queryFn: ({ signal }) => leaderboardApi.page(page, signal),
        placeholderData: keepPreviousData,
    });

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-5xl flex-col gap-5 px-4 pt-6 pb-16 md:px-6"
        >
            <Breadcrumb
                items={[{ label: "Trang chủ", href: ROUTES.home }, { label: "Bảng xếp hạng" }]}
            />
            <div>
                <h1 className="text-[28px] font-extrabold tracking-tight">Bảng xếp hạng</h1>
                <p className="mt-1 text-[15px] text-muted-foreground">
                    Rating sau các contest tính rating.
                </p>
            </div>
            {isPending ? (
                <div role="status" aria-label="Đang tải bảng xếp hạng">
                    <Skeleton className="h-96" />
                </div>
            ) : null}
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
                <div className="rounded-lg border bg-card">
                    <div className="overflow-x-auto">
                        <LeaderboardTable entries={data.items} viewerId={currentUser?.id} />
                    </div>
                    <div className="border-t">
                        <Pagination result={data} hrefOf={pageHref} unit="người" />
                    </div>
                </div>
            ) : null}
        </main>
    );
}
