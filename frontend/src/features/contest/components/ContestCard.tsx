import { CircleCheck, Clock, ListOrdered, Users } from "lucide-react";
import Link from "next/link";

import { formatMinutes } from "@/lib/contest";
import { formatNumber, formatTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

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
            className={cn(
                "relative flex items-center gap-5 rounded-lg border bg-card p-4 transition-colors focus-within:border-brand hover:border-brand",
                contest.isRegistered && "border-brand bg-brand-soft/40",
            )}
        >
            <div className="flex w-[84px] flex-none flex-col overflow-hidden rounded-lg border border-brand/30 bg-card text-center">
                <span className="bg-brand py-1 text-[13px] font-semibold text-primary-foreground">
                    {day.weekday}
                </span>
                <span className="pt-1 text-[30px] leading-tight font-extrabold">{day.day}</span>
                <span className="pb-1.5 text-[13px] font-semibold text-muted-foreground">
                    {day.month}
                </span>
            </div>
            <div className="flex min-w-0 flex-1 flex-col gap-2">
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
                    {contest.isRegistered ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-brand px-2.5 py-0.5 text-sm font-semibold text-primary-foreground">
                            <CircleCheck aria-hidden="true" className="size-3.5" />
                            Đã đăng ký
                        </span>
                    ) : null}
                </div>
                <ul className="flex flex-wrap items-center gap-x-5 gap-y-1 text-[14px] text-muted-foreground">
                    <li className="inline-flex items-center gap-1.5">
                        <Clock aria-hidden="true" className="size-4" />
                        {`${formatTime(contest.startAt)} - ${formatTime(contest.endAt)}, ${formatMinutes(contest.durationMinutes)}`}
                    </li>
                    <li className="inline-flex items-center gap-1.5">
                        <ListOrdered aria-hidden="true" className="size-4" />
                        {`${formatNumber(contest.problemCount)} bài`}
                    </li>
                    <li className="inline-flex items-center gap-1.5">
                        <Users aria-hidden="true" className="size-4" />
                        {`${formatNumber(contest.registrantCount)} người đăng ký`}
                    </li>
                </ul>
            </div>
        </article>
    );
}
