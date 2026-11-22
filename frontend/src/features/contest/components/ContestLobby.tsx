"use client";

import { Users } from "lucide-react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Markdown } from "@/components/shared/Markdown";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";

import { useContest } from "../hooks/useContests";
import { ContestLobbyHero } from "./ContestLobbyHero";
import { ContestProblemTable } from "./ContestProblemTable";
import { ContestRules } from "./ContestRules";

interface ContestLobbyProps {
    slug: string;
}

export function ContestLobby({ slug }: ContestLobbyProps) {
    const { data: contest, error, isPending, refetch } = useContest(slug);

    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-4 pt-6 pb-16 md:px-6"
        >
            {isPending ? (
                <div role="status" aria-label="Đang tải contest" className="flex flex-col gap-3">
                    <Skeleton className="h-56 rounded-xl" />
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
                    <ContestLobbyHero contest={contest} />
                    <div className="flex flex-wrap gap-6">
                        <div className="flex min-w-0 flex-[999_1_560px] flex-col gap-5">
                            {contest.phase === "UPCOMING" ? (
                                <ContestRules contest={contest} />
                            ) : (
                                <ContestProblemTable slug={contest.slug} />
                            )}
                            {contest.description ? (
                                <section className="rounded-lg border bg-card p-5">
                                    <Markdown>{contest.description}</Markdown>
                                </section>
                            ) : null}
                        </div>
                        <aside className="flex min-w-0 flex-[1_1_260px] flex-col gap-4">
                            <section
                                aria-label="Người tham gia"
                                className="flex items-center justify-between rounded-lg border bg-card p-5"
                            >
                                <span className="inline-flex items-center gap-2 font-bold">
                                    <Users aria-hidden="true" className="size-5" />
                                    Người tham gia
                                </span>
                                <span className="text-[22px] font-extrabold">
                                    {formatNumber(contest.registrantCount)}
                                </span>
                            </section>
                        </aside>
                    </div>
                </>
            ) : null}
        </main>
    );
}
