import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { Markdown } from "@/components/shared/Markdown";
import { ResultTable } from "@/components/shared/ResultTable";
import { formatNumber } from "@/lib/format";

import type { WorkspaceProblem } from "../types";

const MS_PER_SECOND = 1000;

interface ProblemStatementProps {
    problem: WorkspaceProblem;
    meta?: React.ReactNode;
}

export function ProblemStatement({ problem, meta }: ProblemStatementProps) {
    const [example] = problem.examples;

    return (
        <div className="flex flex-col gap-4 px-6 py-5 text-sm leading-relaxed">
            <div>
                <h1 className="text-xl leading-snug font-bold">{problem.title}</h1>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                    <DifficultyLabel difficulty={problem.difficulty} className="text-[13px]" />
                    {meta}
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
                    {`Giới hạn thời gian ${formatNumber(problem.timeLimitMs / MS_PER_SECOND)} giây, ${formatNumber(problem.hiddenTestcaseCount)} testcase ẩn.`}
                </li>
            </ul>
        </div>
    );
}
