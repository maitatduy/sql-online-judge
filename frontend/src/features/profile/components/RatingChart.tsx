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
const STROKE_STOPS = ["var(--chart-1)", "var(--chart-2)", "var(--chart-3)"];
const FILL_COLOR = "var(--chart-2)";
const LATEST_POINT_COLOR = "var(--chart-3)";
const TIER_LINE_COLORS = ["var(--tier-1)", "var(--tier-2)"];

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
    const gradientId = useId().replace(/[^a-zA-Z0-9]/g, "");
    const strokeGradientId = `rating-stroke-${gradientId}`;
    const fillGradientId = `rating-fill-${gradientId}`;
    const lastIndex = points.length - 1;
    const [axisMin, axisMax] = ratingAxisDomain(points);

    function renderDot({ cx = 0, cy = 0, index = 0 }: RatingDotProps) {
        if (index !== lastIndex) {
            return (
                <circle
                    key={index}
                    cx={cx}
                    cy={cy}
                    r={3.5}
                    strokeWidth={2}
                    stroke={`url(#${strokeGradientId})`}
                    className="fill-background"
                />
            );
        }
        return (
            <g key={index}>
                <circle cx={cx} cy={cy} r={11} fill={LATEST_POINT_COLOR} fillOpacity={0.18} />
                <circle
                    cx={cx}
                    cy={cy}
                    r={5.5}
                    fill={LATEST_POINT_COLOR}
                    strokeWidth={2}
                    className="stroke-background"
                />
            </g>
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
                <linearGradient
                    id={strokeGradientId}
                    gradientUnits="userSpaceOnUse"
                    x1="0"
                    y1="0"
                    x2="100%"
                    y2="0"
                >
                    {STROKE_STOPS.map((color, index) => (
                        <stop
                            key={color}
                            offset={index / (STROKE_STOPS.length - 1)}
                            stopColor={color}
                        />
                    ))}
                </linearGradient>
                <linearGradient id={fillGradientId} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0" stopColor={FILL_COLOR} stopOpacity={0.3} />
                    <stop offset="1" stopColor={FILL_COLOR} stopOpacity={0} />
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
                tick={{ fontSize: AXIS_FONT_SIZE_PX, fill: "var(--text-muted)" }}
            />
            <YAxis hide domain={[axisMin, axisMax]} />
            {RATING_TIERS.map((tier, index) => (
                <ReferenceLine
                    key={tier.title}
                    y={tier.minRating}
                    stroke={TIER_LINE_COLORS[index]}
                    strokeOpacity={0.55}
                    strokeDasharray="4 4"
                    ifOverflow="hidden"
                    label={{
                        value: tierRangeLabel(tier),
                        position: "insideBottomLeft",
                        fontSize: AXIS_FONT_SIZE_PX,
                        fill: TIER_LINE_COLORS[index],
                    }}
                />
            ))}
            <Tooltip
                content={() => null}
                cursor={{ stroke: "var(--chart-2)", strokeOpacity: 0.5, strokeDasharray: "3 3" }}
            />
            <Area
                type="monotone"
                dataKey="rating"
                stroke={`url(#${strokeGradientId})`}
                strokeWidth={3}
                fill={`url(#${fillGradientId})`}
                isAnimationActive={false}
                dot={renderDot}
                activeDot={{ r: 6, fill: "var(--chart-2)", stroke: "var(--bg)", strokeWidth: 2 }}
            />
        </AreaChart>
    );
}
