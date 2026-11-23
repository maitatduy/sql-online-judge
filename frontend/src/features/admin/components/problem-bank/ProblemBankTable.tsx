import Link from "next/link";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { formatNumber, formatPercent } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import type { BankProblem } from "../../problem-bank/types";
import { BODY_CELL_CLASS, HEADER_CELL_CLASS } from "../users/table-cells";
import { VisibilityLabel } from "./VisibilityLabel";

const TEXT_COLUMNS = ["#", "Tên bài", "Chủ đề", "Độ khó", "Hiển thị", "Người ra đề"];
const NUMBER_COLUMNS = ["Lượt nộp", "AC"];

interface ProblemBankTableProps {
    problems: BankProblem[];
}

export function ProblemBankTable({ problems }: ProblemBankTableProps) {
    return (
        <div role="region" aria-label="Bảng bài tập" tabIndex={0} className="overflow-x-auto">
            <table
                aria-label="Kho bài tập"
                className="w-full min-w-[860px] border-collapse text-[13px]"
            >
                <thead>
                    <tr>
                        {TEXT_COLUMNS.map((column) => (
                            <th key={column} scope="col" className={HEADER_CELL_CLASS}>
                                {column}
                            </th>
                        ))}
                        {NUMBER_COLUMNS.map((column) => (
                            <th
                                key={column}
                                scope="col"
                                className={`${HEADER_CELL_CLASS} text-right`}
                            >
                                {column}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {problems.map((problem) => (
                        <ProblemBankRow key={problem.id} problem={problem} />
                    ))}
                </tbody>
            </table>
        </div>
    );
}

function ProblemBankRow({ problem }: { problem: BankProblem }) {
    return (
        <tr className="hover:bg-surface/60">
            <td className={`${BODY_CELL_CLASS} text-muted-foreground`}>{problem.number}</td>
            <td className={`${BODY_CELL_CLASS} font-semibold`}>
                {problem.visibility === "PUBLIC" ? (
                    <Link href={ROUTES.problem(problem.slug)} className="hover:text-brand">
                        {problem.title}
                    </Link>
                ) : (
                    problem.title
                )}
            </td>
            <td className={BODY_CELL_CLASS}>
                <span className="flex flex-wrap gap-1">
                    {problem.tags.map((tag) => (
                        <span
                            key={tag.slug}
                            className="rounded-sm bg-surface-2 px-1.5 py-0.5 text-xs"
                        >
                            {tag.name}
                        </span>
                    ))}
                </span>
            </td>
            <td className={BODY_CELL_CLASS}>
                <DifficultyLabel difficulty={problem.difficulty} />
            </td>
            <td className={BODY_CELL_CLASS}>
                {problem.visibility ? <VisibilityLabel visibility={problem.visibility} /> : null}
            </td>
            <td className={`${BODY_CELL_CLASS} text-muted-foreground`}>
                {problem.authorUsername ?? "Không rõ"}
            </td>
            <td className={`${BODY_CELL_CLASS} text-right`}>
                {formatNumber(problem.submissionCount)}
            </td>
            <td className={`${BODY_CELL_CLASS} text-right`}>
                {formatPercent(problem.acceptanceRate)}
            </td>
        </tr>
    );
}
