import { TriangleAlert } from "lucide-react";

import { formatTime } from "@/lib/format";
import { cn } from "@/lib/utils";

interface StaleDataNoticeProps {
    updatedAtMs: number;
    className?: string;
}

export function StaleDataNotice({ updatedAtMs, className }: StaleDataNoticeProps) {
    return (
        <div
            role="status"
            className={cn(
                "flex items-center gap-2.5 rounded-lg border border-status-busy/30 bg-status-busy/12 px-3.5 py-2.5 text-[13px]",
                className,
            )}
        >
            <TriangleAlert aria-hidden="true" className="size-4 shrink-0 text-status-busy" />
            <p>
                {`Chưa cập nhật được số liệu mới. Đang hiện số liệu lúc ${formatTime(new Date(updatedAtMs))}.`}
            </p>
        </div>
    );
}
