"use client";

import { useQueryClient } from "@tanstack/react-query";
import { Trophy } from "lucide-react";

import { Breadcrumb } from "@/components/shared/Breadcrumb";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { CONTESTS_QUERY_KEY, useContests } from "../hooks/useContests";
import { useRefreshWhenDue } from "../hooks/useRefreshWhenDue";
import { ContestCard } from "./ContestCard";
import { FinishedContestTable } from "./FinishedContestTable";
import { LiveContestHero } from "./LiveContestHero";
import { NoLiveContest } from "./NoLiveContest";

export function ContestCatalogue() {
    const running = useContests("RUNNING");
    const upcoming = useContests("UPCOMING");
    const finished = useContests("FINISHED");
    const failed = [running, upcoming, finished].find((query) => query.error && !query.data);
    const queryClient = useQueryClient();
    const nextChange = [running.data?.items[0]?.endAt, upcoming.data?.items[0]?.startAt]
        .filter((moment): moment is string => moment !== undefined)
        .sort()[0];
    useRefreshWhenDue(nextChange, () => {
        void queryClient.invalidateQueries({ queryKey: CONTESTS_QUERY_KEY });
    });

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 pt-7 pb-16 md:px-6"
        >
            <div className="flex flex-col gap-2">
                <Breadcrumb
                    items={[{ label: "Trang chủ", href: ROUTES.home }, { label: "Contest" }]}
                />
                <h1 className="text-[24px] font-extrabold tracking-tight">Contest</h1>
            </div>
            {running.data?.items.map((contest) => (
                <LiveContestHero key={contest.id} contest={contest} />
            ))}
            {running.data?.items.length === 0 ? (
                <NoLiveContest next={upcoming.data?.items[0]} />
            ) : null}
            {failed?.error ? (
                <ErrorState
                    title="Chưa tải được contest"
                    message={failed.error.message}
                    traceId={
                        failed.error instanceof ApiRequestError ? failed.error.traceId : undefined
                    }
                    onRetry={() => void failed.refetch()}
                />
            ) : null}
            <Tabs defaultValue="upcoming" className="gap-4">
                <TabsList>
                    <TabsTrigger value="upcoming">
                        Sắp diễn ra
                        {upcoming.data ? (
                            <span className="text-muted-foreground">
                                {formatNumber(upcoming.data.totalItems)}
                            </span>
                        ) : null}
                    </TabsTrigger>
                    <TabsTrigger value="finished">Đã kết thúc</TabsTrigger>
                </TabsList>
                <TabsContent value="upcoming" className="flex flex-col gap-3">
                    {upcoming.isPending ? <ListSkeleton /> : null}
                    {upcoming.data?.items.length === 0 ? (
                        <EmptyState title="Chưa có contest sắp tới." icon={<Trophy />} />
                    ) : null}
                    {upcoming.data?.items.map((contest) => (
                        <ContestCard key={contest.id} contest={contest} />
                    ))}
                </TabsContent>
                <TabsContent value="finished">
                    {finished.isPending ? <ListSkeleton /> : null}
                    {finished.data?.items.length === 0 ? (
                        <EmptyState title="Chưa có contest nào kết thúc." icon={<Trophy />} />
                    ) : null}
                    {finished.data && finished.data.items.length > 0 ? (
                        <FinishedContestTable contests={finished.data.items} />
                    ) : null}
                </TabsContent>
            </Tabs>
        </main>
    );
}

function ListSkeleton() {
    return (
        <div role="status" aria-label="Đang tải contest" className="flex flex-col gap-3">
            <Skeleton className="h-24 rounded-lg" />
            <Skeleton className="h-24 rounded-lg" />
        </div>
    );
}
