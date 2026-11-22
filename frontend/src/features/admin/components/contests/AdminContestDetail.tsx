"use client";

import Link from "next/link";

import { ArrowLeft } from "lucide-react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";
import { ADMIN_ROUTES } from "@/lib/admin-navigation";
import { formatContestMoment, formatMinutes } from "@/lib/contest";

import type { AdminContest } from "../../contests/types";
import { useAdminContest } from "../../hooks/useAdminContests";
import { AdminContestPhase } from "./AdminContestPhase";
import { ContestProblemList } from "./ContestProblemList";
import { ContestProblemsEditor } from "./ContestProblemsEditor";

interface AdminContestDetailProps {
    contestId: string;
}

function contestFacts(contest: AdminContest): [label: string, value: string][] {
    return [
        ["Bắt đầu", formatContestMoment(contest.startAt)],
        ["Thời lượng", formatMinutes(contest.durationMinutes)],
        ["Tính rating", contest.isRated ? "Có" : "Không"],
        ["Đóng băng", formatMinutes(contest.freezeMinutes)],
        ["Phạt mỗi lần sai", formatMinutes(contest.penaltyMinutes)],
    ];
}

function ContestFacts({ contest }: { contest: AdminContest }) {
    return (
        <section className="rounded-lg border bg-card p-5">
            <h2 className="mb-3 text-base font-bold">Thông tin chung</h2>
            <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                {contestFacts(contest).map(([label, value]) => (
                    <div key={label}>
                        <dt className="text-sm text-muted-foreground">{label}</dt>
                        <dd className="font-semibold">{value}</dd>
                    </div>
                ))}
            </dl>
        </section>
    );
}

export function AdminContestDetail({ contestId }: AdminContestDetailProps) {
    const { data: contest, error, isPending, refetch } = useAdminContest(contestId);

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-5 px-4 pt-6 pb-10 md:px-7">
            <Link
                href={ADMIN_ROUTES.contests}
                className="inline-flex min-h-11 w-fit items-center gap-1.5 text-muted-foreground hover:text-foreground"
            >
                <ArrowLeft aria-hidden="true" className="size-4" />
                Contest
            </Link>
            {isPending ? (
                <div role="status" aria-label="Đang tải contest" className="flex flex-col gap-3">
                    <Skeleton className="h-8 w-64" />
                    <Skeleton className="h-40" />
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
                    <div className="flex flex-col gap-2">
                        <AdminContestPhase phase={contest.phase} />
                        <h1 className="text-[26px] font-extrabold tracking-[-0.02em]">
                            {contest.title}
                        </h1>
                    </div>
                    {contest.phase === "UPCOMING" ? (
                        <ContestProblemsEditor
                            key={contest.id}
                            contest={contest}
                            facts={<ContestFacts contest={contest} />}
                        />
                    ) : (
                        <div className="flex max-w-4xl flex-col gap-5">
                            <ContestFacts contest={contest} />
                            <section className="flex flex-col gap-3 rounded-lg border bg-card p-5">
                                <h2 className="text-base font-bold">Bài trong contest</h2>
                                <ContestProblemList problems={contest.problems} />
                            </section>
                        </div>
                    )}
                </>
            ) : null}
        </main>
    );
}
