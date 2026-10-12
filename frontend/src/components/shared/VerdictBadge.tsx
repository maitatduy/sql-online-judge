import { cn } from "@/lib/utils";
import { VERDICT_LABELS, type Verdict } from "@/lib/verdict";

type VerdictBadgeVariant = "outline" | "solid";

interface VerdictBadgeProps {
    verdict: Verdict;
    detail?: string;
    variant?: VerdictBadgeVariant;
    className?: string;
}

const VARIANT_CLASSES: Record<VerdictBadgeVariant, Record<Verdict, string>> = {
    outline: {
        AC: "border-current text-verdict-ac",
        WA: "border-current text-verdict-wa",
        OM: "border-current text-verdict-om",
        TLE: "border-current text-verdict-tle",
        RE: "border-current text-verdict-re",
        FS: "border-current text-verdict-fs",
        SE: "border-current text-muted-foreground",
    },
    solid: {
        AC: "border-transparent bg-verdict-ac text-background",
        WA: "border-transparent bg-verdict-wa text-background",
        OM: "border-transparent bg-verdict-om text-background",
        TLE: "border-transparent bg-verdict-tle text-background",
        RE: "border-transparent bg-verdict-re text-background",
        FS: "border-transparent bg-verdict-fs text-background",
        SE: "border-transparent bg-muted-foreground text-background",
    },
};

export function VerdictBadge({
    verdict,
    detail,
    variant = "outline",
    className,
}: VerdictBadgeProps) {
    return (
        <span
            title={VERDICT_LABELS[verdict]}
            className={cn(
                "inline-flex h-5.5 items-center rounded-full border px-2 text-xs font-semibold whitespace-nowrap",
                VARIANT_CLASSES[variant][verdict],
                className,
            )}
        >
            {detail ? `${verdict} · ${detail}` : verdict}
        </span>
    );
}
