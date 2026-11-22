"use client";

import { useState } from "react";

import { ArrowDown, ArrowUp, X } from "lucide-react";

import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import type { AdminContest } from "../../contests/types";
import { useContestReadyProblems, useReplaceContestProblems } from "../../hooks/useAdminContests";
import { ContestProblemLibrary } from "./ContestProblemLibrary";
import { ContestProblemList, type ContestProblemItem } from "./ContestProblemList";

const MAX_PROBLEMS = 26;

interface ContestProblemsEditorProps {
    contest: AdminContest;
    facts: React.ReactNode;
}

function moved<T>(items: T[], from: number, to: number): T[] {
    const next = [...items];
    const [item] = next.splice(from, 1);
    next.splice(to, 0, item);
    return next;
}

function sameOrder(left: ContestProblemItem[], right: ContestProblemItem[]): boolean {
    return (
        left.length === right.length &&
        left.every((problem, index) => problem.problemId === right[index].problemId)
    );
}

export function ContestProblemsEditor({ contest, facts }: ContestProblemsEditorProps) {
    const showToast = useToast();
    const ready = useContestReadyProblems();
    const replace = useReplaceContestProblems(contest.id);
    const [selected, setSelected] = useState<ContestProblemItem[]>(contest.problems);
    const available = (ready.data?.items ?? []).filter(
        (problem) => !selected.some((item) => item.problemId === problem.id),
    );

    function save() {
        replace.mutate(
            selected.map((problem) => problem.problemId),
            {
                onSuccess: (saved) => {
                    setSelected(saved.problems);
                    showToast("Đã lưu danh sách bài.");
                },
                onError: (error) => showToast(`Chưa lưu được. ${error.message}`, "error"),
            },
        );
    }

    return (
        <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
            <div className="flex min-w-0 flex-col gap-5">
                {facts}
                <section className="flex flex-col gap-3 rounded-lg border bg-card p-5">
                    <div className="flex items-center justify-between gap-3">
                        <h2 className="text-base font-bold">Bài trong contest</h2>
                        <Button
                            type="button"
                            disabled={replace.isPending || sameOrder(selected, contest.problems)}
                            onClick={save}
                        >
                            Lưu danh sách bài
                        </Button>
                    </div>
                    <ContestProblemList
                        problems={selected}
                        renderActions={(problem, index) => (
                            <div className="flex">
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    aria-label={`Đưa ${problem.title} lên`}
                                    disabled={index === 0}
                                    onClick={() => setSelected(moved(selected, index, index - 1))}
                                >
                                    <ArrowUp aria-hidden="true" />
                                </Button>
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    aria-label={`Đưa ${problem.title} xuống`}
                                    disabled={index === selected.length - 1}
                                    onClick={() => setSelected(moved(selected, index, index + 1))}
                                >
                                    <ArrowDown aria-hidden="true" />
                                </Button>
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="icon"
                                    aria-label={`Bỏ ${problem.title}`}
                                    onClick={() =>
                                        setSelected(selected.filter((_, at) => at !== index))
                                    }
                                >
                                    <X aria-hidden="true" />
                                </Button>
                            </div>
                        )}
                    />
                </section>
            </div>
            <ContestProblemLibrary
                problems={available}
                isFull={selected.length >= MAX_PROBLEMS}
                onAdd={(problem) =>
                    setSelected([
                        ...selected,
                        {
                            problemId: problem.id,
                            title: problem.title,
                            difficulty: problem.difficulty,
                        },
                    ])
                }
            />
        </div>
    );
}
