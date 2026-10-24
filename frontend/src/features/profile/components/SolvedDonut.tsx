import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { DIFFICULTIES, DIFFICULTY_STROKE_CLASSES } from "@/lib/difficulty";
import { formatNumber } from "@/lib/format";

import { donutSegments, totalProblemCount } from "../donut-segments";
import type { ProfileStats } from "../types";

const FULL_CIRCLE_PERCENT = 100;

interface SolvedDonutProps {
    stats: ProfileStats;
}

export function SolvedDonut({ stats }: SolvedDonutProps) {
    const solvedCount = formatNumber(stats.solvedCount);
    const problemCount = formatNumber(totalProblemCount(stats.solvedByDifficulty));

    return (
        <section className="flex min-w-0 flex-[1_1_260px] items-center gap-4.5 rounded-lg border bg-card p-5">
            <h2 className="sr-only">Tiến độ theo độ khó</h2>
            <svg
                viewBox="0 0 140 140"
                role="img"
                aria-label={`Đã giải ${solvedCount} trên ${problemCount} bài`}
                className="size-28 shrink-0"
            >
                <circle
                    cx="70"
                    cy="70"
                    r="52"
                    fill="none"
                    strokeWidth="12"
                    className="stroke-surface-2"
                />
                {donutSegments(stats.solvedByDifficulty).map((segment) => (
                    <circle
                        key={segment.difficulty}
                        cx="70"
                        cy="70"
                        r="52"
                        fill="none"
                        strokeWidth="12"
                        pathLength={FULL_CIRCLE_PERCENT}
                        strokeDasharray={`${segment.percent} ${FULL_CIRCLE_PERCENT - segment.percent}`}
                        strokeDashoffset={-segment.offsetPercent}
                        transform="rotate(-90 70 70)"
                        className={DIFFICULTY_STROKE_CLASSES[segment.difficulty]}
                    />
                ))}
                <text
                    x="70"
                    y="78"
                    textAnchor="middle"
                    className="fill-foreground font-mono text-2xl font-extrabold"
                >
                    {solvedCount}
                </text>
            </svg>
            <ul className="flex flex-1 flex-col gap-2 text-[13px]">
                {DIFFICULTIES.map((difficulty) => (
                    <li key={difficulty} className="flex justify-between gap-3">
                        <DifficultyLabel difficulty={difficulty} />
                        <span className="font-mono">
                            {formatNumber(stats.solvedByDifficulty[difficulty].solved)}/
                            {formatNumber(stats.solvedByDifficulty[difficulty].total)}
                        </span>
                    </li>
                ))}
            </ul>
        </section>
    );
}
