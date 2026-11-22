import { Plus } from "lucide-react";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { Button } from "@/components/ui/button";

import type { ReviewProblemSummary } from "../../problem-reviews/types";

interface ContestProblemLibraryProps {
    problems: ReviewProblemSummary[];
    isFull: boolean;
    onAdd: (problem: ReviewProblemSummary) => void;
}

export function ContestProblemLibrary({ problems, isFull, onAdd }: ContestProblemLibraryProps) {
    return (
        <section
            aria-label="Thêm từ kho bài"
            className="flex flex-col gap-3 rounded-lg border bg-card p-5"
        >
            <h2 className="text-base font-bold">Thêm từ kho bài</h2>
            {problems.length === 0 ? (
                <p className="text-muted-foreground">Không còn bài đã duyệt cho contest.</p>
            ) : (
                <ul className="flex flex-col divide-y">
                    {problems.map((problem) => (
                        <li key={problem.id} className="flex items-center gap-3 py-2.5">
                            <div className="min-w-0 flex-1">
                                <p className="font-semibold">{problem.title}</p>
                                <DifficultyLabel
                                    difficulty={problem.difficulty}
                                    className="text-sm"
                                />
                            </div>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                aria-label={`Thêm ${problem.title}`}
                                disabled={isFull}
                                onClick={() => onAdd(problem)}
                            >
                                <Plus aria-hidden="true" />
                                Thêm
                            </Button>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}
