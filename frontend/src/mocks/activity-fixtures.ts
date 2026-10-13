import { shiftDateKey, vietnamDateKey } from "@/features/profile/activity-heatmap";
import type { ActivityDay, ActivityResponse } from "@/features/profile/types";

const DAY_COUNT = 364;
const DAYS_PER_WEEK = 7;
const QUIET_WEEK_COUNT = 8;
const QUIET_WEEK_COUNT_ON_ACTIVE_DAY = 1;
const SUBMISSION_COUNT_CYCLE = [0, 0, 1, 0, 2, 1, 4, 0, 6];

function submissionCountOn(dayIndex: number): number {
    const weekIndex = Math.floor(dayIndex / DAYS_PER_WEEK);
    if (weekIndex < QUIET_WEEK_COUNT) {
        return (dayIndex * 5) % 4 === 0 ? QUIET_WEEK_COUNT_ON_ACTIVE_DAY : 0;
    }
    const cyclePosition = (dayIndex * 7 + weekIndex * 3 + (dayIndex % 5)) % 9;
    return SUBMISSION_COUNT_CYCLE[cyclePosition];
}

export function buildActiveYear(now: Date): ActivityResponse {
    const todayKey = vietnamDateKey(now);
    const days: ActivityDay[] = [];
    for (let dayIndex = 0; dayIndex < DAY_COUNT; dayIndex += 1) {
        const count = submissionCountOn(dayIndex);
        if (count > 0) {
            days.push({ date: shiftDateKey(todayKey, dayIndex - DAY_COUNT + 1), count });
        }
    }
    return { totalSubmissions: days.reduce((sum, day) => sum + day.count, 0), days };
}

export const EMPTY_ACTIVITY: ActivityResponse = { totalSubmissions: 0, days: [] };
