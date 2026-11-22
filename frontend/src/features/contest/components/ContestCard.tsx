import Link from "next/link";

import { formatMinutes } from "@/lib/contest";
import { formatTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { formatContestDay } from "../countdown";
import type { ContestSummary } from "../types";

interface ContestCardProps {
    contest: ContestSummary;
}

export function ContestCard({ contest }: ContestCardProps) {
    const day = formatContestDay(contest.startAt);

    return (
        <article
            aria-label={contest.title}
            className="relative flex items-center gap-5 rounded-lg border bg-card p-4 transition-colors focus-within:border-brand hover:border-brand"
        >
            <div className="flex w-[76px] flex-none flex-col items-center rounded-lg bg-brand/12 py-2 text-brand">
                <span className="text-sm font-semibold">{day.weekday}</span>
                <span className="text-[28px] leading-tight font-extrabold">{day.day}</span>
                <span className="text-sm">{day.month}</span>
            </div>
            <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-lg font-bold">
                        <Link
                            href={ROUTES.contest(contest.slug)}
                            className="outline-none after:absolute after:inset-0 after:rounded-lg"
                        >
                            {contest.title}
                        </Link>
                    </h3>
                    {contest.isRated ? (
                        <span className="rounded-full bg-brand/12 px-2.5 py-0.5 text-sm font-semibold text-brand">
                            Tính rating
                        </span>
                    ) : null}
                </div>
                <p className="mt-1 text-muted-foreground">
                    {`${formatTime(contest.startAt)} - ${formatTime(contest.endAt)}, ${formatMinutes(contest.durationMinutes)}`}
                </p>
            </div>
        </article>
    );
}
