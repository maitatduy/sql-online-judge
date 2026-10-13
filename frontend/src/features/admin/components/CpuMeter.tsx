import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { type CpuLevel, cpuLevel } from "../worker-status";

const LEVEL_FILL_CLASSES: Record<CpuLevel, string> = {
    normal: "bg-meter-normal",
    high: "bg-meter-high",
    critical: "bg-meter-critical",
};

interface CpuMeterProps {
    cpuPercent: number | null;
}

export function CpuMeter({ cpuPercent }: CpuMeterProps) {
    return (
        <span className="flex items-center gap-2">
            <span
                aria-hidden="true"
                className="h-1.5 flex-1 overflow-hidden rounded-full bg-surface-2"
            >
                {cpuPercent === null ? null : (
                    <span
                        className={cn(
                            "block h-full rounded-full",
                            LEVEL_FILL_CLASSES[cpuLevel(cpuPercent)],
                        )}
                        style={{ width: `${Math.min(cpuPercent, 100)}%` }}
                    />
                )}
            </span>
            <span className="w-8.5 text-right font-mono text-xs">
                {cpuPercent === null ? (
                    <>
                        <span aria-hidden="true">-</span>
                        <span className="sr-only">Không có số liệu CPU</span>
                    </>
                ) : (
                    `${formatNumber(cpuPercent)}%`
                )}
            </span>
        </span>
    );
}
