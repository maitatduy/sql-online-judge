import { ListOrdered } from "lucide-react";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { EmptyState } from "@/components/shared/EmptyState";
import type { Difficulty } from "@/lib/difficulty";

const FIRST_LETTER_CODE = 65;

export interface ContestProblemItem {
    problemId: string;
    title: string;
    difficulty: Difficulty;
}

interface ContestProblemListProps {
    problems: ContestProblemItem[];
    renderActions?: (problem: ContestProblemItem, index: number) => React.ReactNode;
}

export function ContestProblemList({ problems, renderActions }: ContestProblemListProps) {
    if (problems.length === 0) {
        return <EmptyState title="Chưa gắn bài nào." icon={<ListOrdered />} />;
    }
    return (
        <ol aria-label="Bài trong contest" className="flex flex-col gap-2">
            {problems.map((problem, index) => (
                <li
                    key={problem.problemId}
                    className="flex items-center gap-4 rounded-lg border bg-card px-4 py-2.5"
                >
                    <span className="flex size-9 shrink-0 items-center justify-center rounded-md bg-brand/12 text-lg font-extrabold text-brand">
                        {String.fromCharCode(FIRST_LETTER_CODE + index)}
                    </span>
                    <span className="min-w-0 flex-1 font-semibold">{problem.title}</span>
                    <DifficultyLabel difficulty={problem.difficulty} />
                    {renderActions?.(problem, index)}
                </li>
            ))}
        </ol>
    );
}
