import {
    activityLevel,
    formatDateKey,
    type ActivityDay,
    type ActivityLevel,
} from "@/lib/activity-calendar";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

const LEVEL_CLASS_NAMES: Record<ActivityLevel, string> = {
    0: "bg-surface-2",
    1: "bg-heat-low",
    2: "bg-heat-mid",
    3: "bg-brand",
    4: "bg-heat-peak",
};

interface ActivityHeatmapProps {
    weeks: ActivityDay[][];
    total: number;
    unit: string;
}

export function ActivityHeatmap({ weeks, total, unit }: ActivityHeatmapProps) {
    return (
        <div
            role="region"
            aria-label="Lịch hoạt động, cuộn ngang để xem hết"
            tabIndex={0}
            className="overflow-x-auto pb-1"
        >
            <div
                role="img"
                aria-label={`Lịch hoạt động ${formatNumber(weeks.length)} tuần gần nhất, ${formatNumber(total)} ${unit}`}
                className="grid w-max grid-flow-col grid-rows-7 gap-[3px]"
            >
                {weeks.flat().map((day) => {
                    const level = activityLevel(day.count);
                    return (
                        <span
                            key={day.date}
                            title={`${formatDateKey(day.date)}: ${formatNumber(day.count)} ${unit}`}
                            data-level={level}
                            className={cn("size-[11px] rounded-[2px]", LEVEL_CLASS_NAMES[level])}
                        />
                    );
                })}
            </div>
        </div>
    );
}
