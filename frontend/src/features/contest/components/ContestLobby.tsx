"use client";

import Link from "next/link";

import { ArrowLeft, Users } from "lucide-react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Markdown } from "@/components/shared/Markdown";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";
import { CONTEST_PHASE_LABELS, formatContestMoment, formatMinutes } from "@/lib/contest";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import { useContest } from "../hooks/useContests";
import type { ContestDetail } from "../types";
import { ContestRegistrationAction } from "./ContestRegistrationAction";

interface ContestLobbyProps {
    slug: string;
}

function contestFacts(contest: ContestDetail): [label: string, value: string][] {
    return [
        ["Bắt đầu", formatContestMoment(contest.startAt)],
        ["Kết thúc", formatContestMoment(contest.endAt)],
        ["Đóng băng", formatMinutes(contest.freezeMinutes)],
        ["Phạt mỗi lần sai", formatMinutes(contest.penaltyMinutes)],
    ];
}

export function ContestLobby({ slug }: ContestLobbyProps) {
    const { data: contest, error, isPending, refetch } = useContest(slug);

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-4 pt-6 pb-16 md:px-6"
        >
            <Link
                href={ROUTES.contests}
                className="inline-flex min-h-11 w-fit items-center gap-1.5 text-muted-foreground hover:text-foreground"
            >
                <ArrowLeft aria-hidden="true" className="size-4" />
                Contest
            </Link>
            {isPending ? (
                <div role="status" aria-label="Đang tải contest" className="flex flex-col gap-3">
                    <Skeleton className="h-9 w-72" />
                    <Skeleton className="h-32" />
                </div>
            ) : null}
            {error && !contest ? (
                <ErrorState
                    title="Chưa tải được contest"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {contest ? (
                <>
                    <header className="flex flex-col gap-2">
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="rounded-full bg-surface-2 px-3 py-0.5 font-semibold">
                                {CONTEST_PHASE_LABELS[contest.phase]}
                            </span>
                            {contest.isRated ? (
                                <span className="rounded-full bg-brand/12 px-3 py-0.5 font-semibold text-brand">
                                    Tính rating
                                </span>
                            ) : null}
                        </div>
                        <h1 className="text-[28px] leading-tight font-extrabold tracking-tight">
                            {contest.title}
                        </h1>
                        <p className="inline-flex items-center gap-1.5 text-muted-foreground">
                            <Users aria-hidden="true" className="size-4" />
                            {`Đã đăng ký: ${formatNumber(contest.registrantCount)}`}
                        </p>
                    </header>
                    <dl className="grid grid-cols-2 gap-4 rounded-lg border bg-card p-5 sm:grid-cols-4">
                        {contestFacts(contest).map(([label, value]) => (
                            <div key={label}>
                                <dt className="text-muted-foreground">{label}</dt>
                                <dd className="text-lg font-bold">{value}</dd>
                            </div>
                        ))}
                    </dl>
                    <ContestRegistrationAction contest={contest} />
                    {contest.description ? <Markdown>{contest.description}</Markdown> : null}
                </>
            ) : null}
        </main>
    );
}
