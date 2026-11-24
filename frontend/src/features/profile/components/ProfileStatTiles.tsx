import { formatNumber } from "@/lib/format";

import type { RatingSummary, SolvedSummary } from "../types";

const MISSING_VALUE = "-";

interface ProfileStatTilesProps {
    rating: RatingSummary | undefined;
    solved: SolvedSummary | undefined;
}

function valueOf(value: number | null | undefined, prefix = ""): string {
    return value === null || value === undefined ? MISSING_VALUE : prefix + formatNumber(value);
}

export function ProfileStatTiles({ rating, solved }: ProfileStatTilesProps) {
    const tiles = [
        { label: "Bài đã AC", value: valueOf(solved?.solvedCount) },
        { label: "Rating", value: valueOf(rating?.rating) },
        { label: "Hạng toàn cục", value: valueOf(rating?.rank, "#") },
        { label: "Contest đã thi", value: valueOf(rating?.contestsPlayed) },
    ];

    return (
        <dl className="grid grid-cols-[repeat(auto-fit,minmax(160px,1fr))] gap-3.5">
            {tiles.map((tile) => (
                <div key={tile.label} className="rounded-lg border bg-card p-4">
                    <dt className="text-sm text-muted-foreground">{tile.label}</dt>
                    <dd className="mt-1 text-[26px] leading-tight font-extrabold">{tile.value}</dd>
                </div>
            ))}
        </dl>
    );
}
