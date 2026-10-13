import type { ActivityDay } from "./types";

const DAY_MS = 86_400_000;
const DAYS_PER_WEEK = 7;
const WEEK_COUNT = 52;
const DAYS_FROM_SUNDAY_TO_MONDAY = 6;
const LEVEL_THRESHOLDS = [1, 2, 4, 6];

const vietnamDateFormatter = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
});

export type ActivityLevel = 0 | 1 | 2 | 3 | 4;

export function vietnamDateKey(now: Date): string {
    return vietnamDateFormatter.format(now);
}

export function shiftDateKey(dateKey: string, dayOffset: number): string {
    const shifted = new Date(Date.parse(`${dateKey}T00:00:00Z`) + dayOffset * DAY_MS);
    return shifted.toISOString().slice(0, 10);
}

function daysSinceMonday(dateKey: string): number {
    const weekday = new Date(`${dateKey}T00:00:00Z`).getUTCDay();
    return (weekday + DAYS_FROM_SUNDAY_TO_MONDAY) % DAYS_PER_WEEK;
}

export function buildHeatmapWeeks(days: readonly ActivityDay[], now: Date): ActivityDay[][] {
    const todayKey = vietnamDateKey(now);
    const countByDate = new Map(days.map((day) => [day.date, day.count]));
    const cellCount = (WEEK_COUNT - 1) * DAYS_PER_WEEK + daysSinceMonday(todayKey) + 1;

    const weeks: ActivityDay[][] = [];
    for (let cellIndex = 0; cellIndex < cellCount; cellIndex += 1) {
        const date = shiftDateKey(todayKey, cellIndex - cellCount + 1);
        const weekIndex = Math.floor(cellIndex / DAYS_PER_WEEK);
        weeks[weekIndex] ??= [];
        weeks[weekIndex].push({ date, count: countByDate.get(date) ?? 0 });
    }
    return weeks;
}

export function activityLevel(count: number): ActivityLevel {
    return LEVEL_THRESHOLDS.filter((threshold) => count >= threshold).length as ActivityLevel;
}

export function formatDateKey(dateKey: string): string {
    const [year, month, day] = dateKey.split("-");
    return `${day}/${month}/${year}`;
}
