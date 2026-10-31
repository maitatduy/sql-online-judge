import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

import { PROBLEM_STATUS_LABELS, STATUS_TABS } from "../status-labels";
import type { ProblemStatus, StatusCounts } from "../types";

interface StatusTabsProps {
    activeStatus: ProblemStatus | null;
    counts: StatusCounts | undefined;
    onChange: (status: ProblemStatus | null) => void;
}

interface TabOption {
    status: ProblemStatus | null;
    label: string;
    count: number | undefined;
}

export function StatusTabs({ activeStatus, counts, onChange }: StatusTabsProps) {
    const total = counts ? Object.values(counts).reduce((sum, count) => sum + count, 0) : undefined;
    const options: TabOption[] = [
        { status: null, label: "Tất cả", count: total },
        ...STATUS_TABS.map((status) => ({
            status,
            label: PROBLEM_STATUS_LABELS[status],
            count: counts?.[status],
        })),
    ];

    return (
        <div role="tablist" aria-label="Lọc theo trạng thái" className="flex flex-wrap gap-1">
            {options.map(({ status, label, count }) => (
                <button
                    key={label}
                    type="button"
                    role="tab"
                    aria-selected={activeStatus === status}
                    onClick={() => onChange(status)}
                    className={cn(
                        "inline-flex h-9 items-center gap-1.5 border-b-2 px-3 text-sm font-semibold transition-colors focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none max-md:min-h-11",
                        activeStatus === status
                            ? "border-brand text-foreground"
                            : "border-transparent text-muted-foreground hover:text-foreground",
                    )}
                >
                    {label}
                    {count === undefined ? null : " "}
                    {count === undefined ? null : (
                        <span className="rounded-sm bg-surface-2 px-1.5 font-mono text-xs">
                            {formatNumber(count)}
                        </span>
                    )}
                </button>
            ))}
        </div>
    );
}
