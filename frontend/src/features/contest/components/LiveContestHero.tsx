import { CircleCheck } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { formatMinutes } from "@/lib/contest";
import { formatNumber } from "@/lib/format";
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
            className="grid items-center gap-8 rounded-xl border border-contest-hero-line bg-contest-hero p-6 text-contest-hero-text md:p-10 lg:grid-cols-[minmax(0,1fr)_auto]"
        >
            <div className="flex min-w-0 flex-col gap-3">
                <div className="flex flex-wrap items-center gap-2">
                    <ContestPhasePill phase={contest.phase} />
                    {contest.isRegistered ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-contest-hero-panel px-3 py-1 text-sm font-semibold text-contest-hero-accent">
                            <CircleCheck aria-hidden="true" className="size-4" />
                            Đã đăng ký
                        </span>
                    ) : null}
                </div>
                <h2 className="text-[36px] leading-tight font-extrabold tracking-tight">
                    {contest.title}
                </h2>
                <p className="text-lg text-contest-hero-muted">
                    {`${formatMinutes(contest.durationMinutes)}, ${formatNumber(contest.problemCount)} bài, ${formatNumber(contest.registrantCount)} người đăng ký`}
                    {contest.isRated ? ", tính rating" : ""}
                </p>
            </div>
            <div className="flex flex-col gap-4 lg:items-end">
                <Countdown label="Còn lại" target={contest.endAt} />
                <Button asChild size="lg" className="h-12 px-8 text-base">
                    <Link href={ROUTES.contest(contest.slug)}>Vào thi</Link>
                </Button>
            </div>
        </section>
    );
}
