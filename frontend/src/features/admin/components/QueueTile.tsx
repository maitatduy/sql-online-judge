import { TriangleAlert } from "lucide-react";

import { formatNumber } from "@/lib/format";

import { formatWaitDuration } from "../format";
import type { JudgeQueueSummary } from "../types";
import { StatTile } from "./StatTile";

interface QueueTileProps {
    queue: JudgeQueueSummary;
}

export function QueueTile({ queue }: QueueTileProps) {
    return (
        <StatTile
            label="Hàng đợi chấm"
            value={
                queue.isDegraded ? (
                    <span className="inline-flex items-center gap-2 text-status-busy">
                        {formatNumber(queue.size)}
                        <TriangleAlert aria-hidden="true" className="size-5" />
                        <span className="sr-only">Cao hơn bình thường</span>
                    </span>
                ) : (
                    formatNumber(queue.size)
                )
            }
        >
            <p className="mt-1 text-xs font-semibold text-muted-foreground">
                Chờ trung bình {formatWaitDuration(queue.averageWaitMs)}
            </p>
        </StatTile>
    );
}
