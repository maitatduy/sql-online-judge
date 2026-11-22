import Link from "next/link";

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
        <section className="flex flex-wrap items-end gap-8 rounded-xl bg-contest-hero px-6 py-8 text-contest-hero-text md:px-8">
            <div className="min-w-0 flex-[1_1_380px]">
                <Link href={ROUTES.contests} className="text-contest-hero-accent hover:underline">
                    Contest
                </Link>
                <div className="mt-3">
                    <ContestPhasePill phase={contest.phase} />
                </div>
                <h1 className="mt-3 text-[36px] leading-tight font-extrabold tracking-tight">
                    {contest.title}
                </h1>
                <p className="mt-1 text-contest-hero-muted">{scheduleOf(contest)}</p>
                <ul className="mt-4 flex flex-wrap gap-2">
                    {tags.map((tag) => (
                        <li
                            key={tag}
                            className="rounded-md bg-contest-hero-panel px-2.5 py-1 font-semibold text-contest-hero-muted"
                        >
                            {tag}
                        </li>
                    ))}
                </ul>
            </div>
            {contest.phase === "FINISHED" ? null : (
                <div className="flex flex-col items-start gap-4">
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
