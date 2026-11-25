import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { JudgeQueue } from "../../judge/types";
import { StatTile } from "../StatTile";

interface JudgeQueueTilesProps {
    queue: JudgeQueue;
}

interface Tile {
    label: string;
    value: number;
    note: string;
    isAlert: boolean;
}

function tilesOf(queue: JudgeQueue): Tile[] {
    return [
        {
            label: "Đang chờ chấm",
            value: queue.waiting,
            note: "Bài trong hàng đợi",
            isAlert: false,
        },
        { label: "Đang chấm", value: queue.judging, note: "Worker đang xử lý", isAlert: false },
        {
            label: "Bị kẹt",
            value: queue.stuck,
            note: `Chờ quá ${formatNumber(queue.stuckAfterMinutes)} phút`,
            isAlert: queue.stuck > 0,
        },
        {
            label: "Lỗi hệ thống chấm",
            value: queue.systemErrorsLastDay,
            note: "SE trong 24 giờ qua",
            isAlert: queue.systemErrorsLastDay > 0,
        },
        {
            label: "Đã chấm",
            value: queue.judgedLastHour,
            note: "Trong 1 giờ qua",
            isAlert: false,
        },
    ];
}

export function JudgeQueueTiles({ queue }: JudgeQueueTilesProps) {
    return (
        <div className="grid grid-cols-[repeat(auto-fit,minmax(170px,1fr))] gap-3.5">
            {tilesOf(queue).map((tile) => (
                <StatTile
                    key={tile.label}
                    label={tile.label}
                    value={
                        <span className={cn(tile.isAlert && "text-status-busy")}>
                            {formatNumber(tile.value)}
                        </span>
                    }
                >
                    <p className="mt-1 text-xs font-semibold text-muted-foreground">{tile.note}</p>
                </StatTile>
            ))}
        </div>
    );
}
