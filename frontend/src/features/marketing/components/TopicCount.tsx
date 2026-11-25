import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

interface TopicCountProps {
    count: number | undefined;
    className?: string;
}

export function TopicCount({ count, className }: TopicCountProps) {
    return (
        <span className={cn("block", className)}>
            {count === undefined ? "Đang đếm bài…" : `${formatNumber(count)} bài có máy chấm`}
        </span>
    );
}
