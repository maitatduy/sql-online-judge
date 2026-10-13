import { ArrowDown, ArrowUp, Minus } from "lucide-react";

import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

interface TrendDeltaProps {
    changePercent: number;
    note: string;
}

function trendOf(changePercent: number) {
    if (changePercent > 0) {
        return { label: "Tăng", icon: ArrowUp, className: "text-trend-up" };
    }
    if (changePercent < 0) {
        return { label: "Giảm", icon: ArrowDown, className: "text-trend-down" };
    }
    return { label: "Không đổi", icon: Minus, className: "text-muted-foreground" };
}

export function TrendDelta({ changePercent, note }: TrendDeltaProps) {
    const { label, icon: Icon, className } = trendOf(changePercent);
    const amount = changePercent === 0 ? "" : ` ${formatNumber(Math.abs(changePercent))}%`;

    return (
        <p className={cn("mt-1 flex items-center gap-1 text-xs font-semibold", className)}>
            <Icon aria-hidden="true" className="size-3.5 shrink-0" />
            <span>{`${label}${amount} ${note}`}</span>
        </p>
    );
}
