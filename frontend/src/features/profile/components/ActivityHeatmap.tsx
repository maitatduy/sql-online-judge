import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { activityLevel, formatDateKey, type ActivityLevel } from "../activity-heatmap";
import type { ActivityDay } from "../types";

const LEVEL_CLASS_NAMES: Record<ActivityLevel, string> = {
    0: "bg-surface-2",
    1: "bg-heat-low",
    2: "bg-heat-mid",
    3: "bg-brand",
    4: "bg-heat-peak",
};

interface ActivityHeatmapProps {
    weeks: ActivityDay[][];
    totalSubmissions: number;
}

export function ActivityHeatmap({ weeks, totalSubmissions }: ActivityHeatmapProps) {
    return (
        <div
            role="region"
            aria-label="Lịch hoạt động, cuộn ngang để xem hết"
            tabIndex={0}
            className="overflow-x-auto pb-1"
        >
            <div
                role="img"
                aria-label={`Lịch hoạt động ${formatNumber(weeks.length)} tuần gần nhất, ${formatNumber(totalSubmissions)} bài nộp`}
                className="grid w-max grid-flow-col grid-rows-7 gap-[3px]"
            >
                {weeks.flat().map((day) => {
                    const level = activityLevel(day.count);
                    return (
                        <span
                            key={day.date}
                            title={`${formatDateKey(day.date)}: ${formatNumber(day.count)} bài nộp`}
                            data-level={level}
                            className={cn("size-[11px] rounded-[2px]", LEVEL_CLASS_NAMES[level])}
                        />
                    );
                })}
            </div>
        </div>
    );
}
