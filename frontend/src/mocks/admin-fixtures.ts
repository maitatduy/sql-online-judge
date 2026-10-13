import type { AdminOverviewResponse, MonthlySubmissions } from "@/features/admin/types";
import type { AdminNavigationCounts } from "@/lib/admin-navigation";
import type { Verdict } from "@/lib/verdict";

const MINUTE_MS = 60_000;
const DISTRIBUTION_VERDICTS: Verdict[] = ["AC", "WA", "OM", "TLE", "RE", "FS"];
const VERDICT_PERCENTS = [43, 35, 5, 7, 8, 2];
const VERDICT_COUNTS = [552348, 449586, 64227, 89917, 102762, 25690];
const HOUR_MS = 60 * MINUTE_MS;
const VIETNAM_UTC_OFFSET_MS = 7 * HOUR_MS;
const MONTHLY_COUNTS = [
    86420, 79310, 98210, 104560, 112340, 96780, 88150, 101920, 118470, 124630, 109870, 93540,
];
const WEEKLY_CHANGE_NOTE = "so với tuần trước";

export const ADMIN_NAVIGATION_COUNTS: AdminNavigationCounts = {
    abuseReports: 12,
    supportMessages: 3,
    problemReviews: 5,
    hasLiveContest: true,
    hasJudgeIncident: true,
};

function buildMonthlySubmissions(now: Date): MonthlySubmissions {
    const vietnamNow = new Date(now.getTime() + VIETNAM_UTC_OFFSET_MS);
    const currentMonth = vietnamNow.getUTCMonth() + 1;

    return {
        year: vietnamNow.getUTCFullYear(),
        months: MONTHLY_COUNTS.slice(0, currentMonth).map((count, index) => ({
            month: index + 1,
            count,
        })),
    };
}

export function buildAdminOverview(now: Date = new Date()): AdminOverviewResponse {
    return {
        users: { value: 48260, changePercent: 2, changeNote: WEEKLY_CHANGE_NOTE },
        submissions: { value: 1284530, changePercent: 4, changeNote: WEEKLY_CHANGE_NOTE },
        aiMessages: { value: 3905110, changePercent: 5, changeNote: WEEKLY_CHANGE_NOTE },
        queue: { size: 146, averageWaitMs: 3800, isDegraded: true },
        submissionTotals: { allTime: 1284530, today: 9807, last7Days: 44307 },
        monthlySubmissions: buildMonthlySubmissions(now),
        verdictDistribution: DISTRIBUTION_VERDICTS.map((verdict, index) => ({
            verdict,
            count: VERDICT_COUNTS[index],
            percent: VERDICT_PERCENTS[index],
        })),
    };
}
