"use client";

import Link from "next/link";

import { ArrowLeft, ListOrdered } from "lucide-react";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";
import { ADMIN_ROUTES } from "@/lib/admin-navigation";

import { CONTEST_PHASE_LABELS, formatContestMoment, formatMinutes } from "../../contests/format";
import type { AdminContest } from "../../contests/types";
import { useAdminContest } from "../../hooks/useAdminContests";

interface AdminContestDetailProps {
    contestId: string;
}

function contestFacts(contest: AdminContest): [label: string, value: string][] {
    return [
        ["Giai đoạn", CONTEST_PHASE_LABELS[contest.phase]],
        ["Bắt đầu", formatContestMoment(contest.startAt)],
        ["Kết thúc", formatContestMoment(contest.endAt)],
        ["Đóng băng", formatMinutes(contest.freezeMinutes)],
        ["Phạt mỗi lần sai", formatMinutes(contest.penaltyMinutes)],
        ["Tính rating", contest.isRated ? "Có" : "Không"],
    ];
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
                    <div>
                        <h1 className="text-[24px] font-extrabold tracking-[-0.02em]">
                            {contest.title}
                        </h1>
                        <p className="mt-1 text-muted-foreground">{contest.slug}</p>
                    </div>
                    <dl className="grid gap-4 rounded-lg border bg-card p-5 sm:grid-cols-3">
                        {contestFacts(contest).map(([label, value]) => (
                            <div key={label}>
                                <dt className="text-sm text-muted-foreground">{label}</dt>
                                <dd className="font-semibold">{value}</dd>
                            </div>
                        ))}
                    </dl>
                    <section className="flex flex-col gap-3">
                        <h2 className="text-base font-bold">Bài trong contest</h2>
                        {contest.problems.length === 0 ? (
                            <EmptyState title="Chưa gắn bài nào." icon={<ListOrdered />} />
                        ) : (
                            <ol
                                aria-label="Bài trong contest"
                                className="divide-y rounded-lg border bg-card"
                            >
                                {contest.problems.map((problem) => (
                                    <li
                                        key={problem.problemId}
                                        className="flex items-center gap-4 px-4 py-3"
                                    >
                                        <span className="flex size-8 items-center justify-center rounded-md bg-surface-2 font-bold">
                                            {problem.letter}
                                        </span>
                                        <span className="flex-1 font-semibold">
                                            {problem.title}
                                        </span>
                                        <DifficultyLabel difficulty={problem.difficulty} />
                                    </li>
                                ))}
                            </ol>
                        )}
                    </section>
                </>
            ) : null}
        </main>
    );
}
