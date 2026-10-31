"use client";

import { useState } from "react";

import { DifficultyLabel } from "@/components/shared/DifficultyLabel";
import { formatRelativeTime } from "@/lib/format";
import { cn } from "@/lib/utils";

import { PROBLEM_STATUS_LABELS } from "../status-labels";
import type { StudioProblemSummary } from "../types";

const CELL_CLASS = "border-b px-4 py-3 align-middle";
const HEADER_CLASS = "border-b px-4 py-2.5 text-left text-xs font-semibold text-muted-foreground";

interface MyProblemsTableProps {
    problems: StudioProblemSummary[];
}

export function MyProblemsTable({ problems }: MyProblemsTableProps) {
    const [now] = useState(() => new Date());

    return (
        <div className="overflow-x-auto rounded-lg border bg-card">
            <table
                aria-label="Bài của tôi"
                className="w-full min-w-[640px] border-collapse text-sm"
            >
                <thead>
                    <tr>
                        <th scope="col" className={HEADER_CLASS}>
                            Tên bài
                        </th>
                        <th scope="col" className={cn(HEADER_CLASS, "w-36")}>
                            Trạng thái
                        </th>
                        <th scope="col" className={cn(HEADER_CLASS, "w-28")}>
                            Độ khó
                        </th>
                        <th scope="col" className={cn(HEADER_CLASS, "w-36")}>
                            Cập nhật
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {problems.map((problem) => (
                        <tr key={problem.id}>
                            <td className={cn(CELL_CLASS, "font-semibold")}>{problem.title}</td>
                            <td className={CELL_CLASS}>
                                <span className="rounded-full bg-surface-2 px-2.5 py-0.5 text-xs font-semibold">
                                    {PROBLEM_STATUS_LABELS[problem.status]}
                                </span>
                            </td>
                            <td className={CELL_CLASS}>
                                <DifficultyLabel
                                    difficulty={problem.difficulty}
                                    className="text-[13px]"
                                />
                            </td>
                            <td className={cn(CELL_CLASS, "text-[13px] text-muted-foreground")}>
                                <time dateTime={problem.updatedAt}>
                                    {formatRelativeTime(problem.updatedAt, now)}
                                </time>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
