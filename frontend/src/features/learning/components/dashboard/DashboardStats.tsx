import { Skeleton } from "@/components/shared/Skeleton";
import { formatNumber, formatPercent } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { RatingSummary, SubmissionStats } from "../../types";

interface DashboardStatsProps {
    stats: SubmissionStats | undefined;
    rating: RatingSummary | undefined;
}

interface StatTile {
    label: string;
    value: string | undefined;
    note: string;
    isTrend?: boolean;
}

const FULL_PERCENT = 100;
const NO_VALUE = "-";

function submissionTiles(stats: SubmissionStats | undefined): StatTile[] {
    if (!stats) {
        return [
            { label: "Bài đã AC", value: undefined, note: "" },
            { label: "Tỉ lệ AC", value: undefined, note: "" },
        ];
    }
    const acceptanceRate =
        stats.attemptCount === 0 ? 0 : (stats.solvedCount / stats.attemptCount) * FULL_PERCENT;
    return [
        {
            label: "Bài đã AC",
            value: formatNumber(stats.solvedCount),
            note: `+${formatNumber(stats.solvedThisWeek)} tuần này`,
            isTrend: stats.solvedThisWeek > 0,
        },
        {
            label: "Tỉ lệ AC",
            value: formatPercent(acceptanceRate),
            note: `${formatNumber(stats.attemptCount)} lần thử`,
        },
    ];
}

function ratingTiles(rating: RatingSummary | undefined): StatTile[] {
    if (!rating) {
        return [
            { label: "Rating contest", value: undefined, note: "" },
            { label: "Hạng toàn cục", value: undefined, note: "" },
        ];
    }
    const isRated = rating.rating !== null;
    return [
        {
            label: "Rating contest",
            value: isRated ? formatNumber(rating.rating ?? 0) : NO_VALUE,
            note: isRated
                ? `${formatNumber(rating.contestsPlayed)} contest đã thi`
                : "Chưa thi contest tính rating",
        },
        {
            label: "Hạng toàn cục",
            value: rating.rank === null ? NO_VALUE : `#${formatNumber(rating.rank)}`,
            note: rating.rank === null ? "Có hạng sau contest đầu tiên" : "Theo rating",
        },
    ];
}

export function DashboardStats({ stats, rating }: DashboardStatsProps) {
    return (
        <ul aria-label="Thống kê" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[...submissionTiles(stats), ...ratingTiles(rating)].map((tile) => (
                <li key={tile.label} className="flex flex-col gap-1 rounded-lg border bg-card p-5">
                    <span className="text-sm text-muted-foreground">{tile.label}</span>
                    {tile.value === undefined ? (
                        <Skeleton className="my-1 h-8 w-24" />
                    ) : (
                        <span className="text-[30px] leading-tight font-extrabold tracking-tight">
                            {tile.value}
                        </span>
                    )}
                    <span
                        className={cn(
                            "min-h-5 text-sm",
                            tile.isTrend ? "font-semibold text-trend-up" : "text-muted-foreground",
                        )}
                    >
                        {tile.note}
                    </span>
                </li>
            ))}
        </ul>
    );
}
