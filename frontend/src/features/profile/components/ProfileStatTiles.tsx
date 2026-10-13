import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { ProfileStats } from "../types";

const MISSING_VALUE = "-";

interface ProfileStatTilesProps {
    rating: number;
    stats: ProfileStats;
}

export function ProfileStatTiles({ rating, stats }: ProfileStatTilesProps) {
    const tiles = [
        { label: "Bài đã AC", value: formatNumber(stats.solvedCount) },
        { label: "Rating", value: formatNumber(rating) },
        {
            label: "Hạng toàn cục",
            value: stats.globalRank === null ? MISSING_VALUE : `#${formatNumber(stats.globalRank)}`,
        },
        {
            label: "Chuỗi ngày",
            value: formatNumber(stats.currentStreakDays),
            valueClassName: "text-streak",
        },
    ];

    return (
        <dl className="grid grid-cols-[repeat(auto-fit,minmax(160px,1fr))] gap-3.5">
            {tiles.map((tile) => (
                <div key={tile.label} className="rounded-lg border bg-card p-4">
                    <dt className="text-xs text-muted-foreground">{tile.label}</dt>
                    <dd
                        className={cn(
                            "mt-1 font-mono text-[26px] leading-tight font-extrabold",
                            tile.valueClassName,
                        )}
                    >
                        {tile.value}
                    </dd>
                </div>
            ))}
        </dl>
    );
}
