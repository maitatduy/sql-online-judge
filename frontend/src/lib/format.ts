const MS_PER_SECOND = 1000;
const JUST_NOW_LABEL = "Vừa xong";
const RELATIVE_UNITS: [unit: Intl.RelativeTimeFormatUnit, seconds: number][] = [
    ["year", 31_536_000],
    ["month", 2_592_000],
    ["week", 604_800],
    ["day", 86_400],
    ["hour", 3_600],
    ["minute", 60],
];

const numberFormatter = new Intl.NumberFormat("vi-VN");
const percentFormatter = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 });
const relativeTimeFormatter = new Intl.RelativeTimeFormat("vi", { numeric: "always" });
const dateFormatter = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Ho_Chi_Minh",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
});

export function formatNumber(value: number): string {
    return numberFormatter.format(value);
}

export function formatPercent(percent: number): string {
    return `${percentFormatter.format(percent)}%`;
}

export function formatDate(date: string | Date): string {
    return dateFormatter.format(new Date(date));
}

export function formatRelativeTime(date: string | Date, now: Date): string {
    const elapsedSeconds = (now.getTime() - new Date(date).getTime()) / MS_PER_SECOND;
    const largestUnit = RELATIVE_UNITS.find(([, seconds]) => elapsedSeconds >= seconds);
    if (!largestUnit) {
        return JUST_NOW_LABEL;
    }
    const [unit, seconds] = largestUnit;
    return relativeTimeFormatter.format(-Math.floor(elapsedSeconds / seconds), unit);
}
