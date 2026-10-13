import { formatNumber } from "@/lib/format";

import type { AdminOverviewResponse, MetricSummary } from "../types";
import { QueueTile } from "./QueueTile";
import { StatTile } from "./StatTile";
import { TrendDelta } from "./TrendDelta";

interface OverviewStatTilesProps {
    overview: AdminOverviewResponse;
}

interface MetricTileProps {
    label: string;
    metric: MetricSummary;
}

function MetricTile({ label, metric }: MetricTileProps) {
    return (
        <StatTile label={label} value={formatNumber(metric.value)}>
            <TrendDelta changePercent={metric.changePercent} note={metric.changeNote} />
        </StatTile>
    );
}

export function OverviewStatTiles({ overview }: OverviewStatTilesProps) {
    return (
        <div className="grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-4">
            <MetricTile label="Tổng người dùng" metric={overview.users} />
            <MetricTile label="Bài nộp" metric={overview.submissions} />
            <QueueTile queue={overview.queue} />
            <MetricTile label="Tin nhắn trợ lý AI" metric={overview.aiMessages} />
        </div>
    );
}
