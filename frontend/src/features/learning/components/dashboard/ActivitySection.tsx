import { ActivityHeatmap } from "@/components/shared/ActivityHeatmap";
import { Skeleton } from "@/components/shared/Skeleton";
import { buildHeatmapWeeks, longestStreakDays, type ActivityDay } from "@/lib/activity-calendar";
import { formatNumber } from "@/lib/format";

import { DashboardSection } from "./DashboardSection";

interface ActivitySectionProps {
    days: ActivityDay[] | undefined;
    now: Date;
}

const LEGEND_CLASSES = ["bg-surface-2", "bg-heat-low", "bg-heat-mid", "bg-brand", "bg-heat-peak"];

export function ActivitySection({ days, now }: ActivitySectionProps) {
    const total = (days ?? []).reduce((sum, day) => sum + day.count, 0);

    return (
        <DashboardSection
            title="Hoạt động 12 tháng"
            className="flex-[2_1_560px]"
            aside={
                days ? (
                    <span className="text-sm text-muted-foreground">
                        {`${formatNumber(total)} hoạt động, chuỗi dài nhất ${formatNumber(longestStreakDays(days))} ngày`}
                    </span>
                ) : null
            }
        >
            {days ? (
                <>
                    <ActivityHeatmap
                        weeks={buildHeatmapWeeks(days, now)}
                        total={total}
                        unit="hoạt động"
                    />
                    <div
                        aria-hidden="true"
                        className="flex items-center justify-end gap-1 text-sm text-muted-foreground"
                    >
                        Ít
                        {LEGEND_CLASSES.map((levelClass) => (
                            <span
                                key={levelClass}
                                className={`size-3 rounded-[2px] ${levelClass}`}
                            />
                        ))}
                        Nhiều
                    </div>
                </>
            ) : (
                <Skeleton className="h-28" />
            )}
        </DashboardSection>
    );
}
