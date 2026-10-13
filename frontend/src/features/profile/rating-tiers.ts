import { formatNumber } from "@/lib/format";

const AXIS_STEP = 50;
const HEADROOM_ABOVE_CEILING = 100;

export interface RatingTier {
    title: string;
    minRating: number;
    maxRating?: number;
}

export const RATING_TIERS: readonly RatingTier[] = [
    { title: "Chuyên viên", minRating: 1400, maxRating: 1599 },
    { title: "Chuyên gia", minRating: 1600 },
];

interface RatedPoint {
    rating: number;
}

export function peakRating(points: readonly RatedPoint[]): number {
    return Math.max(...points.map((point) => point.rating));
}

export function ratingAxisDomain(points: readonly RatedPoint[]): [number, number] {
    const lowestRating = Math.min(...points.map((point) => point.rating));
    const peak = peakRating(points);
    const nextTier = RATING_TIERS.find((tier) => tier.minRating > peak);
    const ceiling = nextTier ? nextTier.minRating : peak;
    return [
        Math.floor((lowestRating - AXIS_STEP) / AXIS_STEP) * AXIS_STEP,
        Math.ceil((ceiling + HEADROOM_ABOVE_CEILING) / AXIS_STEP) * AXIS_STEP,
    ];
}

export function tierRangeLabel(tier: RatingTier): string {
    return tier.maxRating === undefined
        ? `${tier.title} ≥ ${formatNumber(tier.minRating)}`
        : `${tier.title} ${formatNumber(tier.minRating)}-${formatNumber(tier.maxRating)}`;
}

export function ratingChange(points: readonly RatedPoint[], index: number): number | null {
    return index === 0 ? null : points[index].rating - points[index - 1].rating;
}
