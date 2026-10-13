import type { MonthlyCount } from "./types";

const MONTHS_PER_YEAR = 12;
const FULL_HEIGHT_PERCENT = 100;

export interface MonthlyBar {
    month: number;
    count: number | null;
    heightPercent: number;
    isPeak: boolean;
}

export function monthlyBars(months: MonthlyCount[]): MonthlyBar[] {
    const highestCount = Math.max(0, ...months.map((entry) => entry.count));
    const peakMonth = months.find((entry) => highestCount > 0 && entry.count === highestCount);

    return Array.from({ length: MONTHS_PER_YEAR }, (_, index) => {
        const month = index + 1;
        const count = months.find((entry) => entry.month === month)?.count ?? null;
        return {
            month,
            count,
            heightPercent:
                count === null || highestCount === 0
                    ? 0
                    : (count / highestCount) * FULL_HEIGHT_PERCENT,
            isPeak: peakMonth?.month === month,
        };
    });
}
