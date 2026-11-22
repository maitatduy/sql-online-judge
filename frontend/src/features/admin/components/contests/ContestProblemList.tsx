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
        <ol aria-label="Bài trong contest" className="divide-y rounded-lg border bg-card">
            {problems.map((problem, index) => (
                <li key={problem.problemId} className="flex items-center gap-4 px-4 py-2.5">
                    <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-surface-2 font-bold">
                        {String.fromCharCode(FIRST_LETTER_CODE + index)}
                    </span>
                    <span className="flex-1 font-semibold">{problem.title}</span>
                    <DifficultyLabel difficulty={problem.difficulty} />
                    {renderActions?.(problem, index)}
                </li>
            ))}
        </ol>
    );
}
