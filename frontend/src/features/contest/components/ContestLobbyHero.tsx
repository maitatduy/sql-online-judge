import Link from "next/link";

import { ArrowLeft } from "lucide-react";

import { formatMinutes } from "@/lib/contest";
import { formatDate, formatTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { formatContestDay } from "../countdown";
import type { ContestDetail } from "../types";
import { ContestPhasePill } from "./ContestPhasePill";
import { ContestRegistrationAction } from "./ContestRegistrationAction";
import { Countdown } from "./Countdown";

interface ContestLobbyHeroProps {
    contest: ContestDetail;
}

function scheduleOf(contest: ContestDetail): string {
    const { weekday } = formatContestDay(contest.startAt);
    return `${weekday}, ${formatDate(contest.startAt)}, ${formatTime(contest.startAt)} - ${formatTime(contest.endAt)}`;
}

export function ContestLobbyHero({ contest }: ContestLobbyHeroProps) {
    const tags = [
        ...(contest.isRated ? ["Tính rating"] : []),
        formatMinutes(contest.durationMinutes),
        "ICPC",
    ];

    return (
        <section className="grid items-center gap-8 rounded-xl border border-contest-hero-line bg-contest-hero p-6 text-contest-hero-text md:p-10 lg:grid-cols-[minmax(0,1fr)_auto]">
            <div className="flex min-w-0 flex-col gap-3">
                <Link
                    href={ROUTES.contests}
                    className="inline-flex w-fit items-center gap-1.5 font-semibold text-contest-hero-accent hover:underline"
                >
                    <ArrowLeft aria-hidden="true" className="size-4" />
                    Contest
                </Link>
                <ContestPhasePill phase={contest.phase} />
                <h1 className="text-[40px] leading-tight font-extrabold tracking-tight">
                    {contest.title}
                </h1>
                <p className="text-lg text-contest-hero-muted">{scheduleOf(contest)}</p>
                <ul className="flex flex-wrap gap-2">
                    {tags.map((tag) => (
                        <li
                            key={tag}
                            className="rounded-md border border-contest-hero-line bg-contest-hero-panel px-3 py-1 font-semibold"
                        >
                            {tag}
                        </li>
                    ))}
                </ul>
            </div>
            {contest.phase === "FINISHED" ? null : (
                <div className="flex flex-col gap-4 lg:items-end">
                    <Countdown
                        label={contest.phase === "UPCOMING" ? "Bắt đầu sau" : "Còn lại"}
                        target={contest.phase === "UPCOMING" ? contest.startAt : contest.endAt}
                    />
                    <ContestRegistrationAction contest={contest} />
                </div>
            )}
        </section>
    );
}
