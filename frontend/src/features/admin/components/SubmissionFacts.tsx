import { useId } from "react";

import { formatNumber } from "@/lib/format";

import type { SubmissionTotals, VerdictShare } from "../types";
import { formatVerdictPercent } from "../verdict-appearance";

interface SubmissionFactsProps {
    totals: SubmissionTotals;
    verdictShares: VerdictShare[];
}

interface FactProps {
    label: string;
    value: string;
}

function Fact({ label, value }: FactProps) {
    const labelId = useId();

    return (
        <div role="group" aria-labelledby={labelId} className="min-w-0 flex-1 px-4 first:pl-0">
            <p id={labelId} className="text-xs whitespace-nowrap text-muted-foreground">
                {label}
            </p>
            <p className="mt-0.5 font-mono text-lg font-extrabold">{value}</p>
        </div>
    );
}

export function SubmissionFacts({ totals, verdictShares }: SubmissionFactsProps) {
    const acceptedPercent = verdictShares.find((share) => share.verdict === "AC")?.percent ?? 0;

    return (
        <div className="flex divide-x border-b pb-4">
            <Fact label="Hôm nay" value={formatNumber(totals.today)} />
            <Fact label="7 ngày qua" value={formatNumber(totals.last7Days)} />
            <Fact label="Tỉ lệ AC" value={formatVerdictPercent(acceptedPercent)} />
        </div>
    );
}
