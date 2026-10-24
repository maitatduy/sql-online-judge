import { formatNumber, formatPercent } from "@/lib/format";
import { cn } from "@/lib/utils";
import { VERDICT_STROKE_CLASSES, type Verdict } from "@/lib/verdict";

import type { VerdictShare } from "../types";
import { verdictDonutSegments } from "../verdict-donut";

const FULL_CIRCLE_PERCENT = 100;
const SEGMENT_GAP_PERCENT = 0.6;
const CENTER = 80;
const RADIUS = 60;
const STROKE_WIDTH = 17;
const ACTIVE_STROKE_WIDTH = 21;

interface VerdictDonutProps {
    totalSubmissions: number;
    shares: VerdictShare[];
    activeVerdict: Verdict | null;
    onActiveVerdictChange: (verdict: Verdict | null) => void;
}

interface DonutCenterProps {
    caption: string;
    count: number;
}

function DonutCenter({ caption, count }: DonutCenterProps) {
    return (
        <g aria-hidden="true" className="pointer-events-none">
            <text
                x={CENTER}
                y={CENTER - 12}
                textAnchor="middle"
                className="fill-muted-foreground text-[9px] font-semibold tracking-[0.06em] uppercase"
            >
                {caption}
            </text>
            <text
                x={CENTER}
                y={CENTER + 6}
                textAnchor="middle"
                className="fill-foreground font-mono text-[17px] font-extrabold"
            >
                {formatNumber(count)}
            </text>
            <text
                x={CENTER}
                y={CENTER + 20}
                textAnchor="middle"
                className="fill-muted-foreground text-[10px]"
            >
                bài nộp
            </text>
        </g>
    );
}

export function VerdictDonut({
    totalSubmissions,
    shares,
    activeVerdict,
    onActiveVerdictChange,
}: VerdictDonutProps) {
    const segments = verdictDonutSegments(shares);
    const gapPercent = segments.length > 1 ? SEGMENT_GAP_PERCENT : 0;
    const activeShare = shares.find((share) => share.verdict === activeVerdict);
    const summary = shares
        .map((share) => `${share.verdict} ${formatPercent(share.percent)}`)
        .join(", ");

    return (
        <svg
            viewBox="0 0 160 160"
            role="img"
            aria-label={`Tổng ${formatNumber(totalSubmissions)} bài nộp: ${summary}`}
            className="size-72 shrink-0 lg:size-80"
        >
            <circle
                cx={CENTER}
                cy={CENTER}
                r={RADIUS}
                fill="none"
                strokeWidth={STROKE_WIDTH}
                className="stroke-surface-2"
            />
            {segments.map((segment) => {
                const arcPercent = Math.max(segment.percent - gapPercent, 0);
                const isActive = segment.verdict === activeVerdict;
                return (
                    <circle
                        key={segment.verdict}
                        data-testid="verdict-arc"
                        cx={CENTER}
                        cy={CENTER}
                        r={RADIUS}
                        fill="none"
                        strokeWidth={isActive ? ACTIVE_STROKE_WIDTH : STROKE_WIDTH}
                        pathLength={FULL_CIRCLE_PERCENT}
                        strokeDasharray={`${arcPercent} ${FULL_CIRCLE_PERCENT - arcPercent}`}
                        strokeDashoffset={-segment.offsetPercent}
                        transform={`rotate(-90 ${CENTER} ${CENTER})`}
                        onMouseEnter={() => onActiveVerdictChange(segment.verdict)}
                        onMouseLeave={() => onActiveVerdictChange(null)}
                        className={cn(
                            "transition-[opacity,stroke-width] duration-150 motion-reduce:transition-none",
                            VERDICT_STROKE_CLASSES[segment.verdict],
                            activeShare && !isActive && "opacity-30",
                        )}
                    />
                );
            })}
            {activeShare ? (
                <DonutCenter
                    caption={`${activeShare.verdict} - ${formatPercent(activeShare.percent)}`}
                    count={activeShare.count}
                />
            ) : (
                <DonutCenter caption="Tổng cộng" count={totalSubmissions} />
            )}
        </svg>
    );
}
