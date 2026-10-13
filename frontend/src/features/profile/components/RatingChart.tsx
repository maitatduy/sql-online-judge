"use client";

import { useId } from "react";
import { Area, AreaChart, ReferenceLine, Tooltip, XAxis, YAxis } from "recharts";

import { formatDateKey, vietnamDateKey } from "../activity-heatmap";
import { RATING_TIERS, ratingAxisDomain, tierRangeLabel } from "../rating-tiers";
import type { RatingPoint } from "../types";

const CHART_HEIGHT_PX = 190;
const DAY_PREFIX_LENGTH = "dd/".length;
const AXIS_FONT_SIZE_PX = 12;
const LEADING_GAP_IN_STEPS = 0.45;
const TRAILING_GAP_IN_STEPS = 0.45;
const LINE_COLOR = "var(--chart-1)";
const GUIDE_COLOR = "var(--text-muted)";

interface RatingChartProps {
    points: RatingPoint[];
    onActivePointChange: (pointIndex: number | null) => void;
}

interface RatingDotProps {
    cx?: number;
    cy?: number;
    index?: number;
}

function monthOf(point: RatingPoint): string {
    return formatDateKey(vietnamDateKey(new Date(point.finishedAt))).slice(DAY_PREFIX_LENGTH);
}

export function RatingChart({ points, onActivePointChange }: RatingChartProps) {
    const fillGradientId = `rating-fill-${useId().replace(/[^a-zA-Z0-9]/g, "")}`;
    const lastIndex = points.length - 1;
    const [axisMin, axisMax] = ratingAxisDomain(points);

    function renderLatestDot({ cx = 0, cy = 0, index = 0 }: RatingDotProps) {
        if (index !== lastIndex) {
            return <g key={index} />;
        }
        return (
            <circle
                key={index}
                cx={cx}
                cy={cy}
                r={4.5}
                fill={LINE_COLOR}
                strokeWidth={2}
                className="stroke-background"
            />
        );
    }

    return (
        <AreaChart
            responsive
            accessibilityLayer={false}
            data={points.map((point, index) => ({ ...point, index }))}
            margin={{ top: 12, right: 0, bottom: 0, left: 0 }}
            style={{ width: "100%", height: CHART_HEIGHT_PX }}
            onMouseMove={({ activeTooltipIndex }) =>
                onActivePointChange(
                    activeTooltipIndex === null || activeTooltipIndex === undefined
                        ? null
                        : Number(activeTooltipIndex),
                )
            }
            onMouseLeave={() => onActivePointChange(null)}
        >
            <defs>
                <linearGradient id={fillGradientId} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0" stopColor={LINE_COLOR} stopOpacity={0.16} />
                    <stop offset="1" stopColor={LINE_COLOR} stopOpacity={0} />
                </linearGradient>
            </defs>
            <XAxis
                dataKey="index"
                type="number"
                domain={[-LEADING_GAP_IN_STEPS, lastIndex + TRAILING_GAP_IN_STEPS]}
                height={26}
                axisLine={false}
                tickLine={false}
                ticks={[0, lastIndex]}
                interval={0}
                tickFormatter={(pointIndex: number) => monthOf(points[pointIndex])}
                tick={{ fontSize: AXIS_FONT_SIZE_PX, fill: GUIDE_COLOR }}
            />
            <YAxis hide domain={[axisMin, axisMax]} />
            {RATING_TIERS.map((tier) => (
                <ReferenceLine
                    key={tier.title}
                    y={tier.minRating}
                    stroke={GUIDE_COLOR}
                    strokeOpacity={0.3}
                    strokeDasharray="4 4"
                    ifOverflow="hidden"
                    label={{
                        value: tierRangeLabel(tier),
                        position: "insideBottomLeft",
                        fontSize: AXIS_FONT_SIZE_PX,
                        fill: GUIDE_COLOR,
                    }}
                />
            ))}
            <Tooltip content={() => null} cursor={{ stroke: GUIDE_COLOR, strokeOpacity: 0.45 }} />
            <Area
                type="monotone"
                dataKey="rating"
                stroke={LINE_COLOR}
                strokeWidth={2}
                fill={`url(#${fillGradientId})`}
                isAnimationActive={false}
                dot={renderLatestDot}
                activeDot={{ r: 5, fill: LINE_COLOR, stroke: "var(--bg)", strokeWidth: 2 }}
            />
        </AreaChart>
    );
}
