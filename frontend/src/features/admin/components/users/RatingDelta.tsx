import { ArrowDown, ArrowUp } from "lucide-react";

import { formatNumber } from "@/lib/format";

interface RatingDeltaProps {
    delta: number | null;
}

export function RatingDelta({ delta }: RatingDeltaProps) {
    if (delta === null) {
        return <span className="text-muted-foreground">tạm giữ</span>;
    }
    if (delta === 0) {
        return <span className="text-muted-foreground">Không đổi</span>;
    }
    const isGain = delta > 0;
    const Icon = isGain ? ArrowUp : ArrowDown;

    return (
        <span
            className={`inline-flex items-center gap-0.5 ${isGain ? "text-trend-up" : "text-trend-down"}`}
        >
            <span className="sr-only">{isGain ? "Tăng " : "Giảm "}</span>
            <Icon aria-hidden="true" className="size-3" />
            {`${isGain ? "+" : "-"}${formatNumber(Math.abs(delta))}`}
        </span>
    );
}
