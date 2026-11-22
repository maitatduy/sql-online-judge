import { formatNumber, formatPercent } from "@/lib/format";

import type { ProblemDetail } from "../types";

interface ProblemStatementMetaProps {
    problem: ProblemDetail;
}

export function ProblemStatementMeta({ problem }: ProblemStatementMetaProps) {
    return (
        <>
            {problem.tags.map((tag) => (
                <span
                    key={tag.slug}
                    className="rounded-sm bg-surface-2 px-2 py-0.5 text-xs text-muted-foreground"
                >
                    {tag.name}
                </span>
            ))}
            <span className="ml-1 text-xs text-muted-foreground">
                {`AC ${formatPercent(problem.acceptanceRate)}, ${formatNumber(problem.solvedCount)} người giải`}
            </span>
        </>
    );
}
