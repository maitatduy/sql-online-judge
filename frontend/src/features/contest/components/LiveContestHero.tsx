import Link from "next/link";

import { Button } from "@/components/ui/button";
import { formatMinutes } from "@/lib/contest";
import { ROUTES } from "@/lib/routes";

import type { ContestSummary } from "../types";
import { ContestPhasePill } from "./ContestPhasePill";
import { Countdown } from "./Countdown";

interface LiveContestHeroProps {
    contest: ContestSummary;
}

export function LiveContestHero({ contest }: LiveContestHeroProps) {
    return (
        <section
            aria-label={contest.title}
            className="flex flex-wrap items-center gap-6 rounded-xl bg-contest-hero px-6 py-7 text-contest-hero-text md:px-8"
        >
            <div className="min-w-0 flex-[1_1_340px]">
                <ContestPhasePill phase={contest.phase} />
                <h2 className="mt-3 text-[32px] leading-tight font-extrabold tracking-tight">
                    {contest.title}
                </h2>
                <p className="mt-1 text-contest-hero-muted">
                    {formatMinutes(contest.durationMinutes)}
                    {contest.isRated ? ", tính rating" : ""}
                </p>
            </div>
            <div className="flex flex-wrap items-end gap-4">
                <Countdown label="Còn lại" target={contest.endAt} />
                <Button asChild size="lg">
                    <Link href={ROUTES.contest(contest.slug)}>Vào thi</Link>
                </Button>
            </div>
        </section>
    );
}
