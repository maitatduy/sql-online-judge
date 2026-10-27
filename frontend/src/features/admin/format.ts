import { formatDate, formatTime } from "@/lib/format";

const MS_PER_SECOND = 1000;
const DAY_MS = 24 * 60 * 60_000;
const VIETNAM_OFFSET_MS = 7 * 60 * 60_000;
const VIETNAM_TIME_ZONE = "Asia/Ho_Chi_Minh";
const WEEKDAY_LABELS = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];

const secondsTimeFormatter = new Intl.DateTimeFormat("en-GB", {
    timeZone: VIETNAM_TIME_ZONE,
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
});

const secondsFormatter = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 });

export function formatAuditTime(occurredAt: string): string {
    return `${formatDate(occurredAt)} ${secondsTimeFormatter.format(new Date(occurredAt))}`;
}

export function formatAuditMoment(occurredAt: string, now: Date): string {
    const date = new Date(occurredAt);
    const time = formatTime(date);
    const daysAgo = vietnamDayNumber(now) - vietnamDayNumber(date);
    if (daysAgo === 0) {
        return `Hôm nay ${time}`;
    }
    if (daysAgo === 1) {
        return `Hôm qua ${time}`;
    }
    const shifted = toVietnamClock(date);
    if (shifted.getUTCFullYear() !== toVietnamClock(now).getUTCFullYear()) {
        return `${formatDate(date)} ${time}`;
    }
    const dayMonth = `${twoDigits(shifted.getUTCDate())}/${twoDigits(shifted.getUTCMonth() + 1)}`;
    return `${WEEKDAY_LABELS[shifted.getUTCDay()]} ${dayMonth} ${time}`;
}

function toVietnamClock(date: Date): Date {
    return new Date(date.getTime() + VIETNAM_OFFSET_MS);
}

function vietnamDayNumber(date: Date): number {
    return Math.floor(toVietnamClock(date).getTime() / DAY_MS);
}

function twoDigits(value: number): string {
    return String(value).padStart(2, "0");
}

export function formatWaitDuration(durationMs: number): string {
    return `${secondsFormatter.format(durationMs / MS_PER_SECOND)} giây`;
}
