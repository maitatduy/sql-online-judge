import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { Markdown } from "@/components/shared/Markdown";
import { ResultTable } from "@/components/shared/ResultTable";
import { DIALECT_LABELS } from "@/lib/dialect";
import { formatNumber } from "@/lib/format";

import type { WorkspaceProblem } from "../types";
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

export function ProblemStatement({ problem, meta }: ProblemStatementProps) {
    const [example] = problem.examples;

    return (
        <div className="flex flex-col gap-6 px-6 py-6 text-base leading-relaxed">
            <header className="flex flex-col gap-4">
                <h1 className="text-2xl leading-snug font-extrabold tracking-tight">
                    {problem.title}
                </h1>
                {meta ?? (
                    <DifficultyLabel difficulty={problem.difficulty} className="text-[15px]" />
                )}
            </header>
            <StatementSection title="Đề bài">
                <Markdown>{problem.statementMarkdown}</Markdown>
            </StatementSection>
            <StatementSection title="Input">
                <p>Truy vấn chạy trên các bảng sau, chi tiết kiểu dữ liệu ở tab Schema:</p>
                <ul className="flex flex-col gap-1.5">
                    {problem.schema.map((table) => (
                        <li key={table.name} className="rounded-md bg-surface-2 px-3 py-2">
                            <span className="font-mono font-semibold">{table.name}</span>
                            <span className="font-mono text-sm text-muted-foreground">
                                {` (${table.columns.map((column) => column.name).join(", ")})`}
                            </span>
                        </li>
                    ))}
                </ul>
            </StatementSection>
            <StatementSection title="Output">
                {example ? (
                    <>
                        <p>
                            Kết quả gồm các cột{" "}
                            <span className="font-mono font-semibold">
                                {example.columns.join(", ")}
                            </span>
                            . Ví dụ:
                        </p>
                        <ResultTable label="Ví dụ kết quả mong đợi" result={example} />
                    </>
                ) : (
                    <p>Kết quả theo mô tả ở phần đề bài.</p>
                )}
            </StatementSection>
            <StatementSection title="Điều kiện">
                <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2">
                    <dt className="text-muted-foreground">Câu lệnh được dùng</dt>
                    <dd className="font-semibold">{problem.allowedStatements.join(", ")}</dd>
                    <dt className="text-muted-foreground">Giới hạn thời gian</dt>
                    <dd className="font-semibold">
                        {`${formatNumber(problem.timeLimitMs / MS_PER_SECOND)} giây mỗi testcase`}
                    </dd>
                    <dt className="text-muted-foreground">Hệ quản trị</dt>
                    <dd className="font-semibold">
                        {problem.dialects.map((dialect) => DIALECT_LABELS[dialect]).join(", ")}
                    </dd>
                    <dt className="text-muted-foreground">Testcase ẩn</dt>
                    <dd className="font-semibold">{formatNumber(problem.hiddenTestcaseCount)}</dd>
                </dl>
            </StatementSection>
            <StatementSection title="Chú ý">
                <ul className="flex list-disc flex-col gap-1.5 pl-5">
                    {problem.requiresOrder ? (
                        <li>
                            Bắt buộc đúng thứ tự dòng, đúng dữ liệu nhưng sai thứ tự nhận{" "}
                            <strong className="text-verdict-om">OM</strong>.
                        </li>
                    ) : (
                        <li>Không xét thứ tự dòng, chỉ cần đúng tập kết quả.</li>
                    )}
                    {isReadOnly(problem) ? <li>Mỗi lần nộp chỉ gồm một câu truy vấn.</li> : null}
                </ul>
            </StatementSection>
        </div>
    );
}
