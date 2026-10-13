import type { Verdict } from "@/lib/verdict";

const percentFormatter = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 });

export const VERDICT_FILL_CLASSES: Record<Verdict, string> = {
    AC: "bg-verdict-ac",
    WA: "bg-verdict-wa",
    OM: "bg-verdict-om",
    TLE: "bg-verdict-tle",
    RE: "bg-verdict-re",
    FS: "bg-verdict-fs",
    SE: "bg-muted-foreground",
};

export const VERDICT_STROKE_CLASSES: Record<Verdict, string> = {
    AC: "stroke-verdict-ac",
    WA: "stroke-verdict-wa",
    OM: "stroke-verdict-om",
    TLE: "stroke-verdict-tle",
    RE: "stroke-verdict-re",
    FS: "stroke-verdict-fs",
    SE: "stroke-muted-foreground",
};

export function formatVerdictPercent(percent: number): string {
    return `${percentFormatter.format(percent)}%`;
}
