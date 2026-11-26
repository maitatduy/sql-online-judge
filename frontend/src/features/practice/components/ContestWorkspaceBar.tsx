"use client";

import Link from "next/link";

import { Timer, Trophy } from "lucide-react";

import { contestProblemStatusOf, formatRemaining, type ContestProblemStatus } from "@/lib/contest";
import { ROUTES } from "@/lib/routes";
import { useNow } from "@/lib/useNow";
import { cn } from "@/lib/utils";

import {
    useContestProblemLinks,
    useContestTiming,
    useMyContestStanding,
} from "../hooks/useContestWorkspace";

const STATUS_CLASSES: Record<ContestProblemStatus, string> = {
    ACCEPTED: "border-verdict-ac bg-verdict-ac/12 text-verdict-ac",
    WRONG: "border-verdict-wa bg-verdict-wa/12 text-verdict-wa",
    PENDING: "border-verdict-pending text-verdict-pending",
};

const STATUS_LABELS: Record<ContestProblemStatus, string> = {
    ACCEPTED: "đã AC",
    WRONG: "nộp sai",
    PENDING: "đang chấm",
};

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
    const { data: mine } = useMyContestStanding(slug);

    return (
        <div className="flex flex-wrap items-center gap-x-5 gap-y-2 border-b bg-card px-4 py-2">
            <nav aria-label="Bài trong contest" className="flex gap-1.5">
                {problems?.map((problem) => {
                    const status = contestProblemStatusOf(
                        mine?.cells.find((cell) => cell.letter === problem.letter),
                    );
                    return (
                        <Link
                            key={problem.letter}
                            href={ROUTES.contestProblem(slug, problem.letter)}
                            title={problem.title}
                            aria-current={problem.letter === letter ? "page" : undefined}
                            className={cn(
                                "flex size-9 items-center justify-center rounded-md border font-bold",
                                status && STATUS_CLASSES[status],
                                problem.letter === letter
                                    ? "border-brand bg-brand text-primary-foreground"
                                    : "hover:border-brand hover:text-brand",
                            )}
                        >
                            {problem.letter}
                            {status ? (
                                <span className="sr-only">{`, ${STATUS_LABELS[status]}`}</span>
                            ) : null}
                        </Link>
                    );
                })}
            </nav>
            <span className="flex-1" />
            <Link
                href={ROUTES.contestStandings(slug)}
                className="inline-flex min-h-9 items-center gap-1.5 font-semibold hover:text-brand"
            >
                <Trophy aria-hidden="true" className="size-4" />
                Bảng xếp hạng
            </Link>
            {contest ? <RemainingTime endAt={contest.endAt} /> : null}
        </div>
    );
}
