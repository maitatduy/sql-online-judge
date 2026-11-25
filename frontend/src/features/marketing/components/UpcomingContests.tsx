"use client";

import { CalendarClock, Trophy } from "lucide-react";
import Link from "next/link";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { useUpcomingContests } from "../hooks/useLanding";

const SKELETON_COUNT = 3;
const startFormatter = new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    weekday: "short",
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
});

export function UpcomingContests() {
    const { data, error, isPending, refetch } = useUpcomingContests();
    const contests = data?.items;

    return (
        <section aria-labelledby="upcoming-contests" className="min-w-0 flex-[2_1_560px]">
            <div className="mb-5 flex items-baseline justify-between">
                <h2 id="upcoming-contests" className="text-[28px] font-extrabold">
                    Contest sắp tới
                </h2>
                <Link
                    href={ROUTES.contests}
                    className="inline-flex min-h-11 items-center text-brand underline-offset-4 hover:underline md:min-h-0"
                >
                    Xem tất cả
                </Link>
            </div>
            {isPending ? (
                <div role="status" aria-label="Đang tải contest" className="grid gap-3">
                    {Array.from({ length: SKELETON_COUNT }, (_, index) => (
                        <Skeleton key={index} className="h-20" />
                    ))}
                </div>
            ) : null}
            {!contests && error ? (
                <ErrorState
                    title="Chưa tải được contest"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {contests && contests.length === 0 ? (
                <div className="flex items-center gap-3 rounded-lg border bg-card p-5 text-muted-foreground">
                    <Trophy aria-hidden="true" className="size-5 shrink-0" />
                    Chưa có contest nào sắp diễn ra.
                </div>
            ) : null}
            {contests && contests.length > 0 ? (
                <ul className="grid gap-3">
                    {contests.map((contest) => (
                        <li key={contest.id}>
                            <Link
                                href={ROUTES.contest(contest.slug)}
                                className="flex items-center gap-4 rounded-lg border bg-card p-4 transition-colors hover:border-brand"
                            >
                                <span className="flex size-11 shrink-0 items-center justify-center rounded-lg bg-brand-soft text-brand">
                                    <CalendarClock aria-hidden="true" className="size-5" />
                                </span>
                                <span className="min-w-0 flex-1">
                                    <span className="block truncate font-bold">
                                        {contest.title}
                                    </span>
                                    <span className="text-sm text-muted-foreground">
                                        {`${startFormatter.format(new Date(contest.startAt))}, ${formatNumber(contest.durationMinutes)} phút`}
                                    </span>
                                </span>
                                {contest.isRated ? (
                                    <span className="rounded-full bg-brand-soft px-2.5 py-0.5 text-xs font-semibold text-brand">
                                        Rated
                                    </span>
                                ) : null}
                            </Link>
                        </li>
                    ))}
                </ul>
            ) : null}
        </section>
    );
}
