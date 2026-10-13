import type { AdminNavigationCounts, AdminNavigationIndicator } from "@/lib/admin-navigation";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

const FADE_CLASS = "transition-opacity duration-200 motion-reduce:transition-none";
const PILL_CLASS = "ml-auto rounded-full px-1.75 py-px text-[11px] font-bold text-white";
const ICON_DOT_CLASS = "absolute top-1.5 left-7 size-2 rounded-full";

interface AdminSidebarIndicatorProps {
    indicator: AdminNavigationIndicator;
    counts: AdminNavigationCounts;
    isCompact: boolean;
}

interface IndicatorAppearance {
    fillClassName: string;
    content: React.ReactNode;
    isDotOnly: boolean;
}

function appearanceOf(
    indicator: AdminNavigationIndicator,
    counts: AdminNavigationCounts,
): IndicatorAppearance | null {
    const pendingCount = (count: number, fillClassName: string) =>
        count > 0
            ? {
                  fillClassName,
                  isDotOnly: false,
                  content: (
                      <>
                          {formatNumber(count)}
                          <span className="sr-only"> đang chờ</span>
                      </>
                  ),
              }
            : null;

    switch (indicator) {
        case "abuseReports":
            return pendingCount(counts.abuseReports, "bg-count-report");
        case "supportMessages":
            return pendingCount(counts.supportMessages, "bg-count-support");
        case "problemReviews":
            return pendingCount(counts.problemReviews, "bg-count-review");
        case "liveContest":
            return counts.hasLiveContest
                ? { fillClassName: "bg-count-live", isDotOnly: false, content: "Live" }
                : null;
        case "judgeIncident":
            return counts.hasJudgeIncident
                ? {
                      fillClassName: "bg-status-down",
                      isDotOnly: true,
                      content: <span className="sr-only">Có worker mất kết nối</span>,
                  }
                : null;
    }
}

export function AdminSidebarIndicator({
    indicator,
    counts,
    isCompact,
}: AdminSidebarIndicatorProps) {
    const appearance = appearanceOf(indicator, counts);
    if (!appearance) {
        return null;
    }
    const { fillClassName, content, isDotOnly } = appearance;

    return (
        <>
            <span
                aria-hidden="true"
                className={cn(ICON_DOT_CLASS, FADE_CLASS, fillClassName, !isCompact && "opacity-0")}
            />
            <span
                className={cn(
                    isDotOnly ? "ml-auto size-2 shrink-0 rounded-full" : PILL_CLASS,
                    FADE_CLASS,
                    fillClassName,
                    isCompact && "opacity-0",
                )}
            >
                {content}
            </span>
        </>
    );
}
