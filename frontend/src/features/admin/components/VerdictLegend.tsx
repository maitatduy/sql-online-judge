import { formatNumber, formatPercent } from "@/lib/format";
import { cn } from "@/lib/utils";
import { VERDICT_FILL_CLASSES, VERDICT_LABELS, type Verdict } from "@/lib/verdict";

import type { VerdictShare } from "../types";

const FULL_WIDTH_PERCENT = 100;

interface VerdictLegendProps {
    shares: VerdictShare[];
    activeVerdict: Verdict | null;
    onActiveVerdictChange: (verdict: Verdict | null) => void;
}

export function VerdictLegend({
    shares,
    activeVerdict,
    onActiveVerdictChange,
}: VerdictLegendProps) {
    const largestPercent = Math.max(...shares.map((share) => share.percent), 0);

    return (
        <ul aria-label="Phân bố kết quả chấm" className="flex flex-col gap-0.5">
            {shares.map((share) => (
                <li
                    key={share.verdict}
                    title={VERDICT_LABELS[share.verdict]}
                    onMouseEnter={() => onActiveVerdictChange(share.verdict)}
                    onMouseLeave={() => onActiveVerdictChange(null)}
                    className={cn(
                        "-mx-2 rounded-md px-2 py-1.5 transition-colors",
                        share.verdict === activeVerdict && "bg-surface",
                    )}
                >
                    <div className="flex items-baseline gap-2 text-[13px]">
                        <span
                            aria-hidden="true"
                            className={cn(
                                "size-2.5 shrink-0 self-center rounded-[3px]",
                                VERDICT_FILL_CLASSES[share.verdict],
                            )}
                        />
                        <span className="w-8 shrink-0 font-semibold">{share.verdict}</span>
                        <span className="min-w-0 flex-1 truncate text-muted-foreground">
                            {VERDICT_LABELS[share.verdict]}
                        </span>
                        <span className="font-mono text-muted-foreground">
                            {formatNumber(share.count)}
                        </span>
                        <strong className="w-12 shrink-0 text-right font-mono">
                            {formatPercent(share.percent)}
                        </strong>
                    </div>
                    <div
                        aria-hidden="true"
                        className="mt-1 h-1 overflow-hidden rounded-full bg-surface-2"
                    >
                        <span
                            className={cn(
                                "block h-full rounded-full",
                                VERDICT_FILL_CLASSES[share.verdict],
                            )}
                            style={{
                                width:
                                    largestPercent === 0
                                        ? 0
                                        : `${(share.percent / largestPercent) * FULL_WIDTH_PERCENT}%`,
                            }}
                        />
                    </div>
                </li>
            ))}
        </ul>
    );
}
