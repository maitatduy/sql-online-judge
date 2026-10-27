import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { Markdown } from "@/components/shared/Markdown";
import { ResultTable } from "@/components/shared/ResultTable";
import { formatNumber, formatPercent } from "@/lib/format";

import type { ProblemDetail } from "../types";

const MS_PER_SECOND = 1000;

interface ProblemStatementProps {
    problem: ProblemDetail;
}

export function ProblemStatement({ problem }: ProblemStatementProps) {
    const [example] = problem.examples;

    return (
        <div className="flex flex-col gap-4 px-6 py-5 text-sm leading-relaxed">
            <div>
                <h1 className="text-xl leading-snug font-bold">{problem.title}</h1>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                    <DifficultyLabel difficulty={problem.difficulty} className="text-[13px]" />
                    {problem.tags.map((tag) => (
                        <span
                            key={tag.slug}
                            className="rounded-sm bg-surface-2 px-2 py-0.5 text-xs text-muted-foreground"
                        >
                            {tag.name}
                        </span>
                    ))}
                    <span className="ml-1 text-xs text-muted-foreground">
                        {`AC ${formatPercent(problem.acceptanceRate)} · ${formatNumber(problem.solvedCount)} người giải`}
                    </span>
                </div>
            </div>
            <Markdown>{problem.statementMarkdown}</Markdown>
            {example ? (
                <div className="flex flex-col gap-2">
                    <p className="font-semibold">Ví dụ kết quả mong đợi</p>
                    <ResultTable label="Ví dụ kết quả mong đợi" result={example} />
                </div>
            ) : null}
            <ul className="list-disc pl-5 text-[13px] text-muted-foreground">
                {problem.requiresOrder ? (
                    <li>
                        Bắt buộc đúng thứ tự dòng, sai thứ tự nhận{" "}
                        <strong className="text-verdict-om">OM</strong>.
                    </li>
                ) : null}
                <li>{`Chỉ cho phép ${problem.allowedStatements.join(", ")}.`}</li>
                <li>
                    {`Giới hạn thời gian ${formatNumber(problem.timeLimitMs / MS_PER_SECOND)} giây · ${formatNumber(problem.hiddenTestcaseCount)} testcase ẩn.`}
                </li>
            </ul>
        </div>
    );
}
