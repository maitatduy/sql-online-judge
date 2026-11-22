import Link from "next/link";

import { CalendarClock, Timer } from "lucide-react";

import { formatContestMoment, formatMinutes } from "@/lib/contest";
import { ROUTES } from "@/lib/routes";

import type { ContestSummary } from "../types";

interface ContestCardProps {
    contest: ContestSummary;
}

export function ContestCard({ contest }: ContestCardProps) {
    return (
        <article className="relative flex flex-col gap-3 rounded-lg border bg-card p-5 transition-colors focus-within:border-brand hover:border-brand">
            <h3 className="text-lg leading-snug font-bold">
                <Link
                    href={ROUTES.contest(contest.slug)}
                    className="outline-none after:absolute after:inset-0 after:rounded-lg"
                >
                    {contest.title}
                </Link>
            </h3>
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-muted-foreground">
                <span className="inline-flex items-center gap-1.5">
                    <CalendarClock aria-hidden="true" className="size-4" />
                    {formatContestMoment(contest.startAt)}
                </span>
                <span className="inline-flex items-center gap-1.5">
                    <Timer aria-hidden="true" className="size-4" />
                    {formatMinutes(contest.durationMinutes)}
                </span>
                {contest.isRated ? (
                    <span className="rounded-full bg-brand/12 px-2.5 py-0.5 font-semibold text-brand">
                        Tính rating
                    </span>
                ) : null}
            </div>
        </article>
    );
}
