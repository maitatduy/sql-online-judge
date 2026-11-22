"use client";

import Link from "next/link";

import { ArrowLeft, Timer } from "lucide-react";

import { formatRemaining } from "@/lib/contest";
import { ROUTES } from "@/lib/routes";
import { useNow } from "@/lib/useNow";
import { cn } from "@/lib/utils";

import { useContestProblemLinks, useContestTiming } from "../hooks/useContestWorkspace";

interface ContestWorkspaceBarProps {
    slug: string;
    letter: string;
}

function RemainingTime({ endAt }: { endAt: string }) {
    const now = useNow();
    return (
        <span
            role="timer"
            aria-label="Còn lại"
            className="inline-flex items-center gap-1.5 font-bold tabular-nums"
        >
            <Timer aria-hidden="true" className="size-4 text-muted-foreground" />
            {formatRemaining(endAt, now)}
        </span>
    );
}

export function ContestWorkspaceBar({ slug, letter }: ContestWorkspaceBarProps) {
    const { data: contest } = useContestTiming(slug);
    const { data: problems } = useContestProblemLinks(slug);

    return (
        <div className="flex flex-wrap items-center gap-x-5 gap-y-2 border-b bg-card px-4 py-2">
            <Link
                href={ROUTES.contest(slug)}
                className="inline-flex min-h-9 items-center gap-1.5 font-semibold hover:text-brand"
            >
                <ArrowLeft aria-hidden="true" className="size-4" />
                {contest?.title ?? "Contest"}
            </Link>
            <nav aria-label="Bài trong contest" className="flex gap-1.5">
                {problems?.map((problem) => (
                    <Link
                        key={problem.letter}
                        href={ROUTES.contestProblem(slug, problem.letter)}
                        title={problem.title}
                        aria-current={problem.letter === letter ? "page" : undefined}
                        className={cn(
                            "flex size-9 items-center justify-center rounded-md border font-bold",
                            problem.letter === letter
                                ? "border-brand bg-brand text-primary-foreground"
                                : "hover:border-brand hover:text-brand",
                        )}
                    >
                        {problem.letter}
                    </Link>
                ))}
            </nav>
            <span className="flex-1" />
            {contest ? <RemainingTime endAt={contest.endAt} /> : null}
        </div>
    );
}
