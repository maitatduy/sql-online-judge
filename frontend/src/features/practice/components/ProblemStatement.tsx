import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { Markdown } from "@/components/shared/Markdown";
import { ResultTable } from "@/components/shared/ResultTable";
import { DIALECT_LABELS } from "@/lib/dialect";
import { formatNumber } from "@/lib/format";

import type { WorkspaceProblem } from "../types";
import { StatementSchemaTable } from "./StatementSchemaTable";
import { StatementSection } from "./StatementSection";

const MS_PER_SECOND = 1000;
const READ_ONLY_STATEMENTS = ["SELECT", "WITH"];

interface ProblemStatementProps {
    problem: WorkspaceProblem;
    meta?: React.ReactNode;
}

function isReadOnly(problem: WorkspaceProblem): boolean {
    return problem.allowedStatements.every((statement) => READ_ONLY_STATEMENTS.includes(statement));
}

function constraintsOf(problem: WorkspaceProblem): string[] {
    const constraints = [
        `Câu lệnh được dùng: ${problem.allowedStatements.join(", ")}.`,
        `Giới hạn thời gian: ${formatNumber(problem.timeLimitMs / MS_PER_SECOND)} giây mỗi testcase.`,
        `Hệ quản trị: ${problem.dialects.map((dialect) => DIALECT_LABELS[dialect]).join(", ")}.`,
        `Số testcase ẩn: ${formatNumber(problem.hiddenTestcaseCount)}.`,
        problem.requiresOrder
            ? "Bắt buộc đúng thứ tự dòng, đúng dữ liệu nhưng sai thứ tự nhận OM."
            : "Không xét thứ tự dòng, chỉ cần đúng tập kết quả.",
    ];
    if (isReadOnly(problem)) {
        constraints.push("Mỗi lần nộp chỉ gồm một câu truy vấn.");
    }
    return constraints;
}

export function ProblemStatement({ problem, meta }: ProblemStatementProps) {
    return (
        <div className="flex flex-col gap-5 px-6 py-5 text-[15px] leading-relaxed">
            <header className="flex flex-col gap-3">
                <h1 className="text-xl leading-snug font-extrabold tracking-tight">
                    {problem.title}
                </h1>
                {meta ?? <DifficultyLabel difficulty={problem.difficulty} />}
            </header>
            <section aria-label="Cấu trúc bảng" className="flex flex-col gap-4">
                {problem.schema.map((table) => (
                    <StatementSchemaTable key={table.name} table={table} />
                ))}
            </section>
            <section aria-label="Yêu cầu">
                <Markdown>{problem.statementMarkdown}</Markdown>
            </section>
            {problem.examples.map((example, index) => (
                <StatementSection key={index} title={`Ví dụ ${index + 1}`}>
                    <div className="flex flex-col gap-2 border-l-2 pl-4">
                        <p className="font-semibold">Kết quả:</p>
                        <ResultTable
                            label={`Kết quả mong đợi của ví dụ ${index + 1}`}
                            result={example}
                        />
                    </div>
                </StatementSection>
            ))}
            <StatementSection title="Ràng buộc">
                <ul className="flex list-disc flex-col gap-1 pl-5">
                    {constraintsOf(problem).map((constraint) => (
                        <li key={constraint}>{constraint}</li>
                    ))}
                </ul>
            </StatementSection>
        </div>
    );
}
