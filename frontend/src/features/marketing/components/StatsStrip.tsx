"use client";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";

import { useLandingStats } from "../hooks/useLanding";

const STAT_COUNT = 4;

export function StatsStrip() {
    const { stats, isPending, error, refetch } = useLandingStats();

    return (
        <section aria-label="Số liệu nổi bật" className="border-y bg-surface">
            <div className="mx-auto max-w-7xl px-6 py-8">
                {stats ? (
                    <dl className="grid grid-cols-2 gap-6 md:grid-cols-4">
                        {stats.map((stat) => (
                            <div key={stat.label} className="flex flex-col-reverse">
                                <dt className="mt-0.5 text-muted-foreground">{stat.label}</dt>
                                <dd className="text-[36px] leading-tight font-extrabold">
                                    {formatNumber(stat.value)}
                                </dd>
                            </div>
                        ))}
                    </dl>
                ) : null}
                {!stats && isPending ? (
                    <div
                        role="status"
                        aria-label="Đang tải số liệu"
                        className="grid grid-cols-2 gap-6 md:grid-cols-4"
                    >
                        {Array.from({ length: STAT_COUNT }, (_, index) => (
                            <Skeleton key={index} className="h-16" />
                        ))}
                    </div>
                ) : null}
                {!stats && !isPending && error ? (
                    <ErrorState
                        title="Chưa tải được số liệu"
                        message={error.message}
                        traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                        onRetry={refetch}
                    />
                ) : null}
            </div>
        </section>
    );
}
