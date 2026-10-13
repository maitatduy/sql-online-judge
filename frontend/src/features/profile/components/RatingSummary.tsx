import { TrendingDown, TrendingUp } from "lucide-react";

import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { formatDateKey, vietnamDateKey } from "../activity-heatmap";
import { peakRating, ratingChange } from "../rating-tiers";
import type { RatingPoint } from "../types";

interface RatingSummaryProps {
    points: RatingPoint[];
    activeIndex: number;
}

function RatingChange({ change }: { change: number }) {
    const isGain = change > 0;
    const Icon = isGain ? TrendingUp : TrendingDown;

    return (
        <span
            className={cn(
                "inline-flex items-center gap-1 text-xs font-semibold",
                isGain ? "text-trend-up" : "text-trend-down",
            )}
        >
            <Icon aria-hidden="true" className="size-3.5" />
            {isGain ? "Tăng" : "Giảm"} {formatNumber(Math.abs(change))}
        </span>
    );
}

export function RatingSummary({ points, activeIndex }: RatingSummaryProps) {
    const point = points[activeIndex];
    const change = ratingChange(points, activeIndex);
    const finishedOn = formatDateKey(vietnamDateKey(new Date(point.finishedAt)));

    return (
        <div className="flex flex-wrap items-start justify-between gap-x-6 gap-y-3">
            <div className="min-w-0">
                <h2 className="text-xs font-medium text-muted-foreground">Rating contest</h2>
                <p className="flex flex-wrap items-center gap-x-2.5">
                    <span className="font-mono text-[28px] leading-tight font-extrabold">
                        {formatNumber(point.rating)}
                    </span>
                    {change ? <RatingChange change={change} /> : null}
                </p>
                <p className="mt-0.5 text-xs text-muted-foreground">
                    {point.contestTitle} · {finishedOn} · Hạng #{formatNumber(point.rank)}
                </p>
            </div>
            <dl className="flex gap-6">
                <div>
                    <dt className="text-xs text-muted-foreground">Cao nhất</dt>
                    <dd className="font-mono text-base font-bold">
                        {formatNumber(peakRating(points))}
                    </dd>
                </div>
                <div>
                    <dt className="text-xs text-muted-foreground">Đã thi</dt>
                    <dd className="text-base font-bold">
                        <span className="font-mono">{formatNumber(points.length)}</span> contest
                    </dd>
                </div>
            </dl>
        </div>
    );
}
