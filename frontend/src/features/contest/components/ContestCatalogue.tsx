"use client";

import { Trophy } from "lucide-react";

import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";

import { useContests } from "../hooks/useContests";
import { ContestCard } from "./ContestCard";
import { FinishedContestTable } from "./FinishedContestTable";
import { LiveContestHero } from "./LiveContestHero";

export function ContestCatalogue() {
    const running = useContests("RUNNING");
    const upcoming = useContests("UPCOMING");
    const finished = useContests("FINISHED");
    const failed = [running, upcoming, finished].find((query) => query.error && !query.data);

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-5xl flex-col gap-6 px-4 pt-7 pb-16 md:px-6"
        >
            <h1 className="text-[24px] font-extrabold tracking-tight">Contest</h1>
            {running.data?.items.map((contest) => (
                <LiveContestHero key={contest.id} contest={contest} />
            ))}
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
