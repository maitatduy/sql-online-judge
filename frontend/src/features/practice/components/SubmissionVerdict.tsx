import Link from "next/link";

import { ResultTable, type RowMark } from "@/components/shared/ResultTable";
import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";
import { VERDICT_LABELS, VERDICT_TEXT_CLASSES, type Verdict } from "@/lib/verdict";

import type { ProblemDetail, Submission, SubmissionDiff } from "../types";

const MS_PER_SECOND = 1000;

interface SubmissionVerdictProps {
    problem: ProblemDetail;
    submission: Submission & { verdict: Verdict };
}

function marksOf(rowIndexes: number[], mark: RowMark): Partial<Record<number, RowMark>> {
    return Object.fromEntries(rowIndexes.map((rowIndex) => [rowIndex, mark]));
}

function summaryOf(problem: ProblemDetail, submission: Submission): string | null {
    const { firstFailedTestcase: failed, totalTestcases, runtimeMs } = submission;
    const runtime = runtimeMs === null ? [] : [`${formatNumber(runtimeMs)} ms`];
    if (submission.verdict === "AC") {
        return [`${totalTestcases}/${totalTestcases} testcase`, ...runtime].join(" · ");
    }
    if (!failed) {
        return null;
    }
    const position = `testcase ${failed.index}/${totalTestcases}`;
    if (submission.verdict === "TLE") {
        return `Quá giới hạn ${formatNumber(problem.timeLimitMs / MS_PER_SECOND)} giây ở ${position}`;
    }
    const lead = submission.verdict === "OM" ? `Sai thứ tự ở ${position}` : `Sai ở ${position}`;
    const rowCounts = failed.diff
        ? [
              failed.diff.missingRows.length > 0
                  ? `thiếu ${failed.diff.missingRows.length} dòng`
                  : null,
              failed.diff.extraRows.length > 0 ? `thừa ${failed.diff.extraRows.length} dòng` : null,
          ].filter((part) => part !== null)
        : [];
    return [lead, ...rowCounts, ...runtime].join(" · ");
}

function DiffTables({ diff }: { diff: SubmissionDiff }) {
    return (
        <div className="grid gap-3 xl:grid-cols-2">
            <ResultTable
                label={`Kết quả của bạn · ${formatNumber(diff.actual.rows.length)} dòng`}
                result={diff.actual}
                rowMarks={marksOf(diff.extraRows, "extra")}
            />
            <ResultTable
                label={`Mong đợi · ${formatNumber(diff.expected.rows.length)} dòng`}
                result={diff.expected}
                rowMarks={marksOf(diff.missingRows, "missing")}
            />
        </div>
    );
}

export function SubmissionVerdict({ problem, submission }: SubmissionVerdictProps) {
    const { verdict, firstFailedTestcase: failed } = submission;
    const summary = summaryOf(problem, submission);

    return (
        <div className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center gap-3">
                <VerdictBadge verdict={verdict} variant="solid" />
                <div>
                    <p className={cn("font-bold", VERDICT_TEXT_CLASSES[verdict])}>
                        {VERDICT_LABELS[verdict]}
                    </p>
                    {summary ? <p className="text-xs text-muted-foreground">{summary}</p> : null}
                </div>
                {verdict === "AC" ? (
                    <Button asChild size="sm" variant="outline" className="ml-auto">
                        <Link href={ROUTES.problems}>Về danh sách bài</Link>
                    </Button>
                ) : null}
            </div>
            {submission.errorMessage ? (
                <pre className="rounded-lg border bg-background px-4 py-3 font-mono text-[13px] whitespace-pre-wrap">
                    {submission.errorMessage}
                </pre>
            ) : null}
            {failed?.diff ? <DiffTables diff={failed.diff} /> : null}
            {failed?.isHidden ? (
                <p className="text-[13px] text-muted-foreground">
                    Testcase ẩn nên không hiện dữ liệu so sánh.
                </p>
            ) : null}
        </div>
    );
}
