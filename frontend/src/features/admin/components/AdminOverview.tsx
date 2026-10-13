"use client";

import { ErrorState } from "@/components/shared/ErrorState";
import { MAIN_CONTENT_ID } from "@/components/shared/SkipLink";
import { ApiRequestError } from "@/lib/api-client";

import { useAdminOverview } from "../hooks/useAdminOverview";
import { JudgeWorkersCard } from "./JudgeWorkersCard";
import { OverviewSkeleton } from "./OverviewSkeleton";
import { OverviewStatTiles } from "./OverviewStatTiles";
import { PendingTasksCard } from "./PendingTasksCard";
import { StaleDataNotice } from "./StaleDataNotice";
import { SubmissionTotalCard } from "./SubmissionTotalCard";

export function AdminOverview() {
    const {
        data: overview,
        dataUpdatedAt,
        error,
        isPending,
        isRefetchError,
        refetch,
    } = useAdminOverview();

    return (
        <main id={MAIN_CONTENT_ID} className="flex flex-col gap-6 px-4 pt-7 pb-12 md:px-8">
            <h1 className="text-[26px] font-extrabold">Tổng quan</h1>
            {isPending ? <OverviewSkeleton /> : null}
            {isRefetchError ? <StaleDataNotice updatedAtMs={dataUpdatedAt} /> : null}
            {overview ? (
                <>
                    <OverviewStatTiles overview={overview} />
                    <SubmissionTotalCard
                        totals={overview.submissionTotals}
                        verdictShares={overview.verdictDistribution}
                        monthly={overview.monthlySubmissions}
                    />
                </>
            ) : null}
            {!overview && error ? (
                <ErrorState
                    title="Chưa tải được số liệu tổng quan"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                />
            ) : null}
            <div className="flex flex-wrap items-start gap-5">
                <JudgeWorkersCard />
                <PendingTasksCard />
            </div>
        </main>
    );
}
