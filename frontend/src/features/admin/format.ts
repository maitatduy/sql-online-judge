import { formatDate } from "@/lib/format";

const MS_PER_SECOND = 1000;
const VIETNAM_TIME_ZONE = "Asia/Ho_Chi_Minh";

const secondsFormatter = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 });

const timeFormatter = new Intl.DateTimeFormat("en-GB", {
    timeZone: VIETNAM_TIME_ZONE,
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
});

export function formatVietnamTime(date: string | Date): string {
    return timeFormatter.format(new Date(date));
}

export function formatAuditTime(occurredAt: string): string {
    return `${formatDate(occurredAt)} ${formatVietnamTime(occurredAt)}`;
}

export function formatWaitDuration(durationMs: number): string {
    return `${secondsFormatter.format(durationMs / MS_PER_SECOND)} giây`;
}
