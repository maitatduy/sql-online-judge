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
const relativeTimeFormatter = new Intl.RelativeTimeFormat("vi", { numeric: "always" });

export function formatNumber(value: number): string {
    return numberFormatter.format(value);
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
