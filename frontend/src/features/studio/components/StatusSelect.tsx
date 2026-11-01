import { NativeSelect } from "@/components/shared/NativeSelect";
import { formatNumber } from "@/lib/format";

import { PROBLEM_STATUS_LABELS, STATUS_FILTERS } from "../status-labels";
import type { ProblemStatus, StatusCounts } from "../types";

interface StatusSelectProps {
    value: ProblemStatus | null;
    counts: StatusCounts | undefined;
    onChange: (status: ProblemStatus | null) => void;
}

function withCount(label: string, count: number | undefined): string {
    return count === undefined ? label : `${label} · ${formatNumber(count)}`;
}

export function StatusSelect({ value, counts, onChange }: StatusSelectProps) {
    const total = counts ? Object.values(counts).reduce((sum, count) => sum + count, 0) : undefined;

    return (
        <NativeSelect
            aria-label="Trạng thái"
            value={value ?? ""}
            onChange={(event) => onChange((event.target.value || null) as ProblemStatus | null)}
        >
            <option value="">{withCount("Mọi trạng thái", total)}</option>
            {STATUS_FILTERS.map((status) => (
                <option key={status} value={status}>
                    {withCount(PROBLEM_STATUS_LABELS[status], counts?.[status])}
                </option>
            ))}
        </NativeSelect>
    );
}
