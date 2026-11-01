"use client";

import Link from "next/link";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { Markdown } from "@/components/shared/Markdown";
import { Skeleton } from "@/components/shared/Skeleton";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";

import { useStudioProblem } from "../hooks/useStudioProblem";
import { PROBLEM_STATUS_LABELS } from "../status-labels";
import { EDITABLE_STATUSES, type StudioProblemSummary } from "../types";

interface ProblemDetailDialogProps {
    problem: StudioProblemSummary;
    onClose: () => void;
}

function ProblemDetailBody({ problemId }: { problemId: string }) {
    const { data: detail, error } = useStudioProblem(problemId);

    if (error) {
        return (
            <p className="text-[13px] text-destructive">Không tải được đề bài. {error.message}</p>
        );
    }
    if (!detail) {
        return (
            <div role="status" aria-label="Đang tải chi tiết bài" className="flex flex-col gap-2">
                <Skeleton className="h-4 w-48" />
                <Skeleton className="h-24" />
            </div>
        );
    }
    return (
        <div className="flex flex-col gap-3">
            <p className="text-[13px] text-muted-foreground">{detail.slug}</p>
            {detail.tags.length > 0 ? (
                <ul aria-label="Chủ đề" className="flex flex-wrap gap-1.5">
                    {detail.tags.map((tag) => (
                        <li
                            key={tag.slug}
                            className="rounded-full bg-surface-2 px-2.5 py-0.5 text-xs"
                        >
                            {tag.name}
                        </li>
                    ))}
                </ul>
            ) : null}
            <div className="max-h-80 overflow-y-auto rounded-md border p-4 text-sm">
                {detail.statementMarkdown.trim() ? (
                    <Markdown>{detail.statementMarkdown}</Markdown>
                ) : (
                    <p className="text-muted-foreground">Chưa có đề bài.</p>
                )}
            </div>
        </div>
    );
}

export function ProblemDetailDialog({ problem, onClose }: ProblemDetailDialogProps) {
    const isEditable = EDITABLE_STATUSES.includes(problem.status);

    return (
        <Dialog open onOpenChange={(open) => (open ? undefined : onClose())}>
            <DialogContent className="sm:max-w-2xl">
                <DialogTitle>{problem.title}</DialogTitle>
                <DialogDescription className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full bg-surface-2 px-2.5 py-0.5 text-xs font-semibold text-foreground">
                        {PROBLEM_STATUS_LABELS[problem.status]}
                    </span>
                    <DifficultyLabel difficulty={problem.difficulty} className="text-[13px]" />
                </DialogDescription>
                <ProblemDetailBody problemId={problem.id} />
                <div className="flex justify-end gap-2">
                    <Button type="button" variant="outline" onClick={onClose}>
                        Đóng
                    </Button>
                    {isEditable ? (
                        <Button asChild>
                            <Link href={`/studio/problems/${problem.id}`}>Sửa bài</Link>
                        </Button>
                    ) : null}
                </div>
            </DialogContent>
        </Dialog>
    );
}
