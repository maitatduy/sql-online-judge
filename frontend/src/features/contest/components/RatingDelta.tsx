import { ArrowDown, ArrowUp } from "lucide-react";

import { cn } from "@/lib/utils";

interface RatingDeltaProps {
    delta: number;
}

export function RatingDelta({ delta }: RatingDeltaProps) {
    if (delta === 0) {
        return <span className="font-semibold text-muted-foreground">0</span>;
    }
    const isUp = delta > 0;
    const Arrow = isUp ? ArrowUp : ArrowDown;
    return (
        <span
            className={cn(
                "inline-flex items-center gap-0.5 font-bold",
                isUp ? "text-trend-up" : "text-trend-down",
            )}
        >
            <Arrow aria-hidden="true" className="size-4" />
            <span aria-hidden="true">{Math.abs(delta)}</span>
            <span className="sr-only">{`${isUp ? "Tăng" : "Giảm"} ${Math.abs(delta)}`}</span>
        </span>
    );
}
