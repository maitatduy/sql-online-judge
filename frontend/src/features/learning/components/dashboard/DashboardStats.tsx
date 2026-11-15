import { Skeleton } from "@/components/shared/Skeleton";
import { formatNumber, formatPercent } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { SubmissionStats } from "../../types";

interface DashboardStatsProps {
    stats: SubmissionStats | undefined;
}

interface StatTile {
    label: string;
    value: string | null;
    note: string;
    isTrend?: boolean;
}

const FULL_PERCENT = 100;
const COMING_SOON = "Sắp có";

function tilesOf(stats: SubmissionStats): StatTile[] {
    const acceptanceRate =
        stats.submissionCount === 0
            ? 0
            : (stats.acceptedCount / stats.submissionCount) * FULL_PERCENT;
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
            note: `${formatNumber(stats.submissionCount)} bài nộp`,
        },
        { label: "Rating contest", value: null, note: "Mở cùng contest" },
        { label: "Hạng toàn cục", value: null, note: "Mở cùng bảng xếp hạng" },
    ];
}

export function DashboardStats({ stats }: DashboardStatsProps) {
    if (!stats) {
        return <Skeleton className="h-28 rounded-lg" />;
    }
    return (
        <ul aria-label="Thống kê" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {tilesOf(stats).map((tile) => (
                <li key={tile.label} className="flex flex-col gap-1 rounded-lg border bg-card p-5">
                    <span className="text-sm text-muted-foreground">{tile.label}</span>
                    {tile.value === null ? (
                        <span className="text-xl font-bold text-muted-foreground">
                            {COMING_SOON}
                        </span>
                    ) : (
                        <span className="text-[30px] leading-tight font-extrabold tracking-tight">
                            {tile.value}
                        </span>
                    )}
                    <span
                        className={cn(
                            "text-sm",
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
