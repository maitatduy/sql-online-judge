"use client";

import { Snowflake, Trophy } from "lucide-react";

import { Breadcrumb } from "@/components/shared/Breadcrumb";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Pagination } from "@/components/shared/Pagination";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useCurrentUser } from "@/lib/useCurrentUser";

import { useContest, useRatingChanges } from "../hooks/useContests";
import { useMyStanding, useStandings, useStandingsEvents } from "../hooks/useStandings";
import { StandingsTable } from "./StandingsTable";

interface ContestStandingsProps {
    slug: string;
    page: number;
}

export function ContestStandings({ slug, page }: ContestStandingsProps) {
    const { data: contest } = useContest(slug);
    const { data: currentUser } = useCurrentUser();
    const { data: standings, error, isPending, refetch } = useStandings(slug, page);
    const { data: mine } = useMyStanding(slug, currentUser !== undefined);
    const { data: ratingChanges } = useRatingChanges(slug);
    const ratingDeltas = new Map(
        (ratingChanges ?? []).map((change) => [change.username, change.delta]),
    );
    useStandingsEvents(slug);

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-7xl flex-col gap-5 px-4 pt-6 pb-16 md:px-6"
        >
            <Breadcrumb
                items={[
                    { label: "Trang chủ", href: ROUTES.home },
                    { label: "Contest", href: ROUTES.contests },
                    { label: contest?.title ?? "Contest", href: ROUTES.contest(slug) },
                    { label: "Bảng xếp hạng" },
                ]}
            />
            <div className="flex flex-wrap items-center gap-3">
                <h1 className="text-[28px] font-extrabold tracking-tight">Bảng xếp hạng</h1>
                {standings?.isFrozen ? (
                    <span className="inline-flex h-7 items-center gap-1.5 rounded-full bg-surface-2 px-3 font-semibold text-muted-foreground">
                        <Snowflake aria-hidden="true" className="size-4" />
                        Đóng băng
                    </span>
                ) : null}
            </div>
            {mine ? (
                <section
                    aria-label="Kết quả của bạn"
                    className="flex flex-wrap items-center gap-x-6 gap-y-1 rounded-lg border bg-card px-5 py-4"
                >
                    <span className="font-bold">Kết quả của bạn</span>
                    <span className="text-lg font-extrabold">{`${mine.solved} bài`}</span>
                    <span className="text-muted-foreground">
                        {`${formatNumber(mine.penaltyMinutes)} phút phạt`}
                    </span>
                </section>
            ) : null}
            {isPending ? (
                <div role="status" aria-label="Đang tải bảng xếp hạng">
                    <Skeleton className="h-64 rounded-lg" />
                </div>
            ) : null}
            {error && !standings ? (
                <ErrorState
                    title="Chưa tải được bảng xếp hạng"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {standings && standings.items.length === 0 ? (
                <EmptyState title="Chưa có ai lên bảng." icon={<Trophy />} />
            ) : null}
            {standings && standings.items.length > 0 ? (
                <>
                    <StandingsTable standings={standings} ratingDeltas={ratingDeltas} />
                    <Pagination
                        result={standings}
                        hrefOf={(target) => `${ROUTES.contestStandings(slug)}?page=${target}`}
                        unit="người"
                    />
                </>
            ) : null}
        </main>
    );
}
