import { DIFFICULTIES, type Difficulty } from "@/lib/difficulty";

import type { SolvedProgress } from "./types";

const FULL_CIRCLE_PERCENT = 100;

export interface DonutSegment {
    difficulty: Difficulty;
    percent: number;
    offsetPercent: number;
}

export function totalProblemCount(solvedByDifficulty: Record<Difficulty, SolvedProgress>): number {
    return DIFFICULTIES.reduce((sum, difficulty) => sum + solvedByDifficulty[difficulty].total, 0);
}

export function donutSegments(
    solvedByDifficulty: Record<Difficulty, SolvedProgress>,
): DonutSegment[] {
    const problemCount = totalProblemCount(solvedByDifficulty);
    if (problemCount === 0) {
        return [];
    }

    const segments: DonutSegment[] = [];
    let offsetPercent = 0;
    DIFFICULTIES.forEach((difficulty) => {
        const percent =
            (solvedByDifficulty[difficulty].solved / problemCount) * FULL_CIRCLE_PERCENT;
        if (percent > 0) {
            segments.push({ difficulty, percent, offsetPercent });
        }
        offsetPercent += percent;
    });
    return segments;
}
