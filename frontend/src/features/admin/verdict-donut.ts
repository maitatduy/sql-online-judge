import type { Verdict } from "@/lib/verdict";

import type { VerdictShare } from "./types";

const FULL_CIRCLE_PERCENT = 100;

export interface VerdictDonutSegment {
    verdict: Verdict;
    percent: number;
    offsetPercent: number;
}

export function verdictDonutSegments(shares: VerdictShare[]): VerdictDonutSegment[] {
    const totalPercent = shares.reduce((sum, share) => sum + share.percent, 0);
    if (totalPercent === 0) {
        return [];
    }

    const segments: VerdictDonutSegment[] = [];
    let offsetPercent = 0;
    shares.forEach((share) => {
        const percent = (share.percent / totalPercent) * FULL_CIRCLE_PERCENT;
        if (percent > 0) {
            segments.push({ verdict: share.verdict, percent, offsetPercent });
        }
        offsetPercent += percent;
    });
    return segments;
}
