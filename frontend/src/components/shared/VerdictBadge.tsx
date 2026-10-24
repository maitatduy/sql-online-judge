import { cn } from "@/lib/utils";
import {
    VERDICT_FILL_CLASSES,
    VERDICT_LABELS,
    VERDICT_TEXT_CLASSES,
    type Verdict,
} from "@/lib/verdict";

type VerdictBadgeVariant = "outline" | "solid";

export type VerdictBadgeStatus = Verdict | "PENDING";

interface VerdictBadgeProps {
    verdict: VerdictBadgeStatus;
    detail?: string;
    variant?: VerdictBadgeVariant;
    className?: string;
}

const PENDING_APPEARANCE = {
    code: "Đang chấm",
    title: "Máy chấm đang chạy testcase",
    outline: "text-verdict-pending",
    solid: "bg-verdict-pending",
};

function appearanceOf(verdict: VerdictBadgeStatus) {
    if (verdict === "PENDING") {
        return PENDING_APPEARANCE;
    }
    return {
        code: verdict,
        title: VERDICT_LABELS[verdict],
        outline: VERDICT_TEXT_CLASSES[verdict],
        solid: VERDICT_FILL_CLASSES[verdict],
    };
}

export function VerdictBadge({
    verdict,
    detail,
    variant = "outline",
    className,
}: VerdictBadgeProps) {
    const appearance = appearanceOf(verdict);

    return (
        <span
            title={appearance.title}
            className={cn(
                "inline-flex h-5.5 items-center rounded-full border px-2 text-xs font-semibold whitespace-nowrap",
                variant === "outline"
                    ? ["border-current", appearance.outline]
                    : ["border-transparent text-background", appearance.solid],
                className,
            )}
        >
            {detail ? `${appearance.code} · ${detail}` : appearance.code}
        </span>
    );
}
