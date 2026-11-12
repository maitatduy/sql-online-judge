"use client";

import { useState } from "react";

import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { Button } from "@/components/ui/button";
import { DIALECT_LABELS } from "@/lib/dialect";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { ProblemSubmissionSummary } from "../types";

import { SubmissionCodeView } from "./SubmissionCodeView";

const CELL_CLASS = "border-b px-3 py-2.5 align-middle";
const COLUMN_COUNT = 5;

interface SubmissionHistoryRowProps {
    submission: ProblemSubmissionSummary;
    now: Date;
}

export function SubmissionHistoryRow({ submission, now }: SubmissionHistoryRowProps) {
    const [isCodeShown, setIsCodeShown] = useState(false);
    const codeRegionId = `submission-code-${submission.id}`;

    return (
        <>
            <tr>
                <td className={cn(CELL_CLASS, "w-24")}>
                    <VerdictBadge verdict={submission.verdict ?? "PENDING"} />
                </td>
                <td className={cn(CELL_CLASS, "text-muted-foreground")}>
                    {DIALECT_LABELS[submission.dialect]}
                </td>
                <td className={CELL_CLASS}>
                    {submission.runtimeMs === null
                        ? "-"
                        : `${formatNumber(submission.runtimeMs)} ms`}
                </td>
                <td
                    className={cn(CELL_CLASS, "text-right whitespace-nowrap text-muted-foreground")}
                >
                    <time dateTime={submission.submittedAt}>
                        {formatRelativeTime(submission.submittedAt, now)}
                    </time>
                </td>
                <td className={cn(CELL_CLASS, "w-24 text-right")}>
                    <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        aria-expanded={isCodeShown}
                        aria-controls={isCodeShown ? codeRegionId : undefined}
                        onClick={() => setIsCodeShown((shown) => !shown)}
                    >
                        {isCodeShown ? "Ẩn mã" : "Xem mã"}
                    </Button>
                </td>
            </tr>
            {isCodeShown ? (
                <tr id={codeRegionId}>
                    <td colSpan={COLUMN_COUNT} className="border-b px-3 py-3">
                        <SubmissionCodeView submissionId={submission.id} />
                    </td>
                </tr>
            ) : null}
        </>
    );
}
