"use client";

import { useId } from "react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";
import { CONTEST_PHASE_LABELS, type ContestPhase } from "@/lib/contest";

import { useContests } from "../hooks/useContests";
import { ContestCard } from "./ContestCard";

const PHASES: ContestPhase[] = ["RUNNING", "UPCOMING", "FINISHED"];
const EMPTY_MESSAGES: Record<ContestPhase, string> = {
    RUNNING: "Không có contest nào đang diễn ra.",
    UPCOMING: "Chưa có contest sắp tới.",
    FINISHED: "Chưa có contest nào kết thúc.",
};

export function ContestCatalogue() {
    return (
        <main
            id={MAIN_CONTENT_ID}
            className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-4 pt-7 pb-16 md:px-6"
        >
            <h1 className="text-[24px] font-extrabold tracking-tight">Contest</h1>
            {PHASES.map((phase) => (
                <ContestPhaseSection key={phase} phase={phase} />
            ))}
        </main>
    );
}

function ContestPhaseSection({ phase }: { phase: ContestPhase }) {
    const headingId = useId();
    const { data, error, isPending, refetch } = useContests(phase);

    return (
        <section aria-labelledby={headingId} className="flex flex-col gap-3">
            <h2 id={headingId} className="text-lg font-bold">
                {CONTEST_PHASE_LABELS[phase]}
            </h2>
            {isPending ? (
                <div role="status" aria-label={`Đang tải contest ${CONTEST_PHASE_LABELS[phase]}`}>
                    <Skeleton className="h-24 rounded-lg" />
                </div>
            ) : null}
            {error && !data ? (
                <ErrorState
                    title="Chưa tải được contest"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            {data && data.items.length === 0 ? (
                <p className="text-muted-foreground">{EMPTY_MESSAGES[phase]}</p>
            ) : null}
            {data && data.items.length > 0 ? (
                <div className="grid gap-4 sm:grid-cols-2">
                    {data.items.map((contest) => (
                        <ContestCard key={contest.id} contest={contest} />
                    ))}
                </div>
            ) : null}
        </section>
    );
}
