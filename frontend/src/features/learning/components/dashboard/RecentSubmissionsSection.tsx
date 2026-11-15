import Link from "next/link";

import { Skeleton } from "@/components/shared/Skeleton";
import { VerdictBadge } from "@/components/shared/VerdictBadge";
import { DIALECT_LABELS } from "@/lib/dialect";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import type { RecentSubmission } from "../../types";
import { DashboardSection } from "./DashboardSection";

interface RecentSubmissionsSectionProps {
    submissions: RecentSubmission[] | undefined;
    now: Date;
}

const HEAD_CELL = "px-4 py-2.5 text-left text-sm font-semibold text-muted-foreground";
const CELL = "border-t px-4 py-3 whitespace-nowrap";

function ProblemCell({ submission }: { submission: RecentSubmission }) {
    if (!submission.problemSlug || !submission.problemTitle) {
        return <span className="text-muted-foreground">Bài đã nộp</span>;
    }
    return (
        <Link
            href={ROUTES.problem(submission.problemSlug)}
            className="font-medium hover:text-brand"
        >
            {`#${submission.problemNumber ?? ""} ${submission.problemTitle}`}
        </Link>
    );
}

export function RecentSubmissionsSection({ submissions, now }: RecentSubmissionsSectionProps) {
    return (
        <DashboardSection title="Bài nộp gần đây" className="flex-[2_1_560px]">
            {submissions === undefined ? <Skeleton className="h-40 rounded-lg" /> : null}
            {submissions?.length === 0 ? (
                <p className="rounded-lg bg-surface-2 p-5 font-semibold">Chưa có bài nộp nào</p>
            ) : null}
            {submissions && submissions.length > 0 ? (
                <div className="-mx-6 overflow-x-auto">
                    <table className="w-full border-collapse">
                        <thead>
                            <tr>
                                <th className={HEAD_CELL}>Kết quả</th>
                                <th className={HEAD_CELL}>Bài</th>
                                <th className={HEAD_CELL}>Dialect</th>
                                <th className={HEAD_CELL}>Thời gian chạy</th>
                                <th className={HEAD_CELL}>Lúc</th>
                            </tr>
                        </thead>
                        <tbody>
                            {submissions.map((submission) => (
                                <tr key={submission.id}>
                                    <td className={CELL}>
                                        <VerdictBadge verdict={submission.verdict ?? "PENDING"} />
                                    </td>
                                    <td className={CELL}>
                                        <ProblemCell submission={submission} />
                                    </td>
                                    <td className={`${CELL} text-muted-foreground`}>
                                        {DIALECT_LABELS[submission.dialect]}
                                    </td>
                                    <td className={CELL}>
                                        {submission.runtimeMs === null
                                            ? ""
                                            : `${formatNumber(submission.runtimeMs)} ms`}
                                    </td>
                                    <td className={`${CELL} text-muted-foreground`}>
                                        {formatRelativeTime(submission.createdAt, now)}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            ) : null}
        </DashboardSection>
    );
}
