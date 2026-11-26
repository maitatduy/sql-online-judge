import { TopicTag } from "@/components/shared/TopicTag";
import { formatNumber, formatPercent } from "@/lib/format";

import type { ProblemDetail } from "../types";

interface ProblemStatementMetaProps {
    problem: ProblemDetail;
}

export function ProblemStatementMeta({ problem }: ProblemStatementMetaProps) {
    return (
        <>
            {problem.tags.map((tag) => (
                <TopicTag key={tag.slug} slug={tag.slug} name={tag.name} />
            ))}
            <span className="ml-1 text-xs text-muted-foreground">
                {`AC ${formatPercent(problem.acceptanceRate)}, ${formatNumber(problem.solvedCount)} người giải`}
            </span>
        </>
    );
}
