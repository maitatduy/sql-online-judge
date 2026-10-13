"use client";

import { useState } from "react";

import { ErrorState } from "@/components/shared/ErrorState";
import { Skeleton } from "@/components/shared/Skeleton";
import { ApiRequestError } from "@/lib/api-client";
import { formatNumber } from "@/lib/format";

import { buildHeatmapWeeks } from "../activity-heatmap";
import { useActivity } from "../hooks/useActivity";

import { ActivityHeatmap } from "./ActivityHeatmap";

interface ActivityHeatmapCardProps {
    username: string;
    longestStreakDays: number;
    now?: Date;
}

export function ActivityHeatmapCard({
    username,
    longestStreakDays,
    now,
}: ActivityHeatmapCardProps) {
    const [mountedAt] = useState(() => new Date());
    const { data: activity, error, isPending, refetch } = useActivity(username);

    return (
        <section className="min-w-0 rounded-lg border bg-card p-5">
            {isPending ? (
                <div role="status" aria-busy="true" aria-label="Đang tải hoạt động">
                    <Skeleton className="h-4 w-2/5" />
                    <Skeleton className="mt-3 h-[95px]" />
                </div>
            ) : null}
            {error && !activity ? (
                <ErrorState
                    title="Không tải được hoạt động"
                    message={error.message}
                    traceId={error instanceof ApiRequestError ? error.traceId : undefined}
                    onRetry={() => void refetch()}
                    className="border-0 p-0"
                />
            ) : null}
            {activity ? (
                <>
                    <div className="mb-3 flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
                        <h2 className="text-base font-bold">
                            {formatNumber(activity.totalSubmissions)} bài nộp trong năm qua
                        </h2>
                        <span className="text-xs text-muted-foreground">
                            Chuỗi dài nhất {formatNumber(longestStreakDays)} ngày
                        </span>
                    </div>
                    <ActivityHeatmap
                        weeks={buildHeatmapWeeks(activity.days, now ?? mountedAt)}
                        totalSubmissions={activity.totalSubmissions}
                    />
                </>
            ) : null}
        </section>
    );
}
