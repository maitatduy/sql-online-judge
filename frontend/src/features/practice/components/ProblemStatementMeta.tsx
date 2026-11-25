import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { TopicTag } from "@/components/shared/TopicTag";
import { formatNumber, formatPercent } from "@/lib/format";

import type { ProblemDetail } from "../types";

interface ProblemStatementMetaProps {
    problem: ProblemDetail;
}

const STAT_CLASS = "flex flex-col gap-1 px-4 py-3";
const LABEL_CLASS = "text-[13px] text-muted-foreground";
const VALUE_CLASS = "text-lg font-extrabold";

export function ProblemStatementMeta({ problem }: ProblemStatementMetaProps) {
    return (
        <div className="flex flex-col gap-3">
            <dl className="grid grid-cols-3 divide-x rounded-lg border bg-card">
                <div className={STAT_CLASS}>
                    <dt className={LABEL_CLASS}>Độ khó</dt>
                    <dd>
                        <DifficultyLabel difficulty={problem.difficulty} className={VALUE_CLASS} />
                    </dd>
                </div>
                <div className={STAT_CLASS}>
                    <dt className={LABEL_CLASS}>Tỉ lệ AC</dt>
                    <dd className={VALUE_CLASS}>{formatPercent(problem.acceptanceRate)}</dd>
                </div>
                <div className={STAT_CLASS}>
                    <dt className={LABEL_CLASS}>Người giải</dt>
                    <dd className={VALUE_CLASS}>{formatNumber(problem.solvedCount)}</dd>
                </div>
            </dl>
            {problem.tags.length > 0 ? (
                <ul aria-label="Chủ đề" className="flex flex-wrap gap-1.5">
                    {problem.tags.map((tag) => (
                        <li key={tag.slug}>
                            <TopicTag slug={tag.slug} name={tag.name} className="text-[13px]" />
                        </li>
                    ))}
                </ul>
            ) : null}
        </div>
    );
}
