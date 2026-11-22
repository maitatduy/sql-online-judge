"use client";

import { useState } from "react";

import { ArrowDown, ArrowUp, X } from "lucide-react";

import { NativeSelect } from "@/components/shared/NativeSelect";
import { useToast } from "@/components/shared/ToastProvider";
import { Button } from "@/components/ui/button";

import type { AdminContest } from "../../contests/types";
import { useContestReadyProblems, useReplaceContestProblems } from "../../hooks/useAdminContests";
import { ContestProblemList, type ContestProblemItem } from "./ContestProblemList";

const MAX_PROBLEMS = 26;

interface ContestProblemsEditorProps {
    contest: AdminContest;
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

export function ContestProblemsEditor({ contest }: ContestProblemsEditorProps) {
    const showToast = useToast();
    const ready = useContestReadyProblems();
    const replace = useReplaceContestProblems(contest.id);
    const [selected, setSelected] = useState<ContestProblemItem[]>(contest.problems);
    const [candidateId, setCandidateId] = useState("");
    const available = (ready.data?.items ?? []).filter(
        (problem) => !selected.some((item) => item.problemId === problem.id),
    );
    const candidate = available.find((problem) => problem.id === candidateId);

    function addCandidate() {
        if (!candidate) {
            return;
        }
        setSelected([
            ...selected,
            { problemId: candidate.id, title: candidate.title, difficulty: candidate.difficulty },
        ]);
        setCandidateId("");
    }

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
        <div className="flex flex-col gap-3">
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
                            onClick={() => setSelected(selected.filter((_, at) => at !== index))}
                        >
                            <X aria-hidden="true" />
                        </Button>
                    </div>
                )}
            />
            <div className="flex flex-wrap items-center gap-2">
                <NativeSelect
                    aria-label="Thêm bài"
                    value={candidateId}
                    disabled={selected.length >= MAX_PROBLEMS}
                    onChange={(event) => setCandidateId(event.target.value)}
                    className="h-10 min-w-64 text-base"
                >
                    <option value="">
                        {available.length > 0
                            ? "Chọn bài đã duyệt cho contest"
                            : "Không còn bài để thêm"}
                    </option>
                    {available.map((problem) => (
                        <option key={problem.id} value={problem.id}>
                            {problem.title}
                        </option>
                    ))}
                </NativeSelect>
                <Button
                    type="button"
                    variant="outline"
                    disabled={!candidate}
                    onClick={addCandidate}
                >
                    Thêm
                </Button>
                <Button
                    type="button"
                    className="ml-auto"
                    disabled={replace.isPending || sameOrder(selected, contest.problems)}
                    onClick={save}
                >
                    Lưu danh sách bài
                </Button>
            </div>
        </div>
    );
}
