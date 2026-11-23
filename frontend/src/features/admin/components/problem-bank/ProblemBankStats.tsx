import { Skeleton } from "@/components/shared/Skeleton";
import { formatNumber } from "@/lib/format";

import { useProblemBankSummary } from "../../hooks/useProblemBank";
import type { BankFilterChange } from "../../hooks/useProblemBankFilters";
import { VISIBILITY_LABELS } from "../../problem-bank/labels";
import type { ProblemBankFilters } from "../../problem-bank/search-params";
import type { ProblemBankSummary } from "../../problem-bank/types";
import type { ProblemVisibility } from "../../problem-reviews/types";
import { SummaryTile } from "../users/SummaryTile";

const VISIBILITY_COUNTS: [ProblemVisibility, keyof ProblemBankSummary][] = [
    ["PUBLIC", "publicCount"],
    ["CONTEST_ONLY", "contestOnlyCount"],
    ["HIDDEN", "hiddenCount"],
];

function countOf(summary: ProblemBankSummary | undefined, key: keyof ProblemBankSummary) {
    return summary ? formatNumber(summary[key]) : <Skeleton className="h-7 w-12" />;
}

interface ProblemBankStatsProps {
    filters: ProblemBankFilters;
    onChange: (change: BankFilterChange) => void;
}

export function ProblemBankStats({ filters, onChange }: ProblemBankStatsProps) {
    const { data: summary } = useProblemBankSummary();

    return (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            {VISIBILITY_COUNTS.map(([visibility, key]) => (
                <SummaryTile
                    key={visibility}
                    label={VISIBILITY_LABELS[visibility]}
                    value={countOf(summary, key)}
                    isSelected={filters.visibility === visibility}
                    onSelect={() =>
                        onChange({
                            visibility: filters.visibility === visibility ? null : visibility,
                        })
                    }
                />
            ))}
            <div className="rounded-lg border bg-card px-4 py-3.5">
                <span className="block text-xs text-muted-foreground">Tỉ lệ AC thấp</span>
                <span className="mt-0.5 block text-[22px] font-extrabold">
                    {countOf(summary, "lowAcceptanceCount")}
                </span>
                <span className="block text-xs text-muted-foreground">
                    Dưới 10%, từ 50 lượt nộp
                </span>
            </div>
        </div>
    );
}
