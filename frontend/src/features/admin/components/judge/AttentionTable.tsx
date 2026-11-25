import Link from "next/link";

import { Button } from "@/components/ui/button";
import { DIALECT_LABELS } from "@/lib/dialect";
import { formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useNow } from "@/lib/useNow";

import { problemLabel } from "../../submissions/labels";
import type { AdminSubmission } from "../../submissions/types";
import { SubmissionResult } from "../submissions/SubmissionResult";
import { BODY_CELL_CLASS, HEADER_CELL_CLASS } from "../users/table-cells";

const COLUMNS = ["Lúc nộp", "Người nộp", "Bài", "Dialect", "Tình trạng"];

interface AttentionTableProps {
    submissions: AdminSubmission[];
    rejudgingId: string | null;
    onOpen: (submissionId: string) => void;
    onRejudge: (submissionId: string) => void;
}

export function AttentionTable({
    submissions,
    rejudgingId,
    onOpen,
    onRejudge,
}: AttentionTableProps) {
    const now = useNow();

    return (
        <div role="region" aria-label="Bảng bài cần xử lý" tabIndex={0} className="overflow-x-auto">
            <table
                aria-label="Bài cần xử lý"
                className="w-full min-w-[820px] border-collapse text-sm"
            >
                <thead>
                    <tr>
                        {COLUMNS.map((column) => (
                            <th key={column} scope="col" className={HEADER_CELL_CLASS}>
                                {column}
                            </th>
                        ))}
                        <th scope="col" className={HEADER_CELL_CLASS}>
                            <span className="sr-only">Thao tác</span>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {submissions.map((submission) => {
                        const label = problemLabel(submission);
                        return (
                            <tr key={submission.id} className="hover:bg-surface">
                                <td className={`${BODY_CELL_CLASS} whitespace-nowrap`}>
                                    {formatRelativeTime(submission.createdAt, now)}
                                </td>
                                <td className={BODY_CELL_CLASS}>
                                    {submission.username ? (
                                        <Link
                                            href={ROUTES.adminUser(submission.username)}
                                            className="font-semibold text-brand hover:underline"
                                        >
                                            {submission.username}
                                        </Link>
                                    ) : (
                                        <span className="text-muted-foreground">
                                            Tài khoản đã xoá
                                        </span>
                                    )}
                                </td>
                                <td className={`${BODY_CELL_CLASS} max-w-72 truncate`}>{label}</td>
                                <td className={BODY_CELL_CLASS}>
                                    {DIALECT_LABELS[submission.dialect]}
                                </td>
                                <td className={BODY_CELL_CLASS}>
                                    <span className="inline-flex items-center gap-2">
                                        <SubmissionResult submission={submission} />
                                        {submission.status === "COMPLETED" ? null : (
                                            <span className="text-[13px] text-muted-foreground">
                                                Bị kẹt
                                            </span>
                                        )}
                                    </span>
                                </td>
                                <td className={`${BODY_CELL_CLASS} whitespace-nowrap text-right`}>
                                    <span className="inline-flex gap-2">
                                        <Button
                                            type="button"
                                            size="sm"
                                            variant="outline"
                                            aria-label={`Xem bài nộp ${label}`}
                                            onClick={() => onOpen(submission.id)}
                                        >
                                            Xem
                                        </Button>
                                        <Button
                                            type="button"
                                            size="sm"
                                            aria-label={`Chấm lại bài nộp ${label}`}
                                            disabled={rejudgingId === submission.id}
                                            onClick={() => onRejudge(submission.id)}
                                        >
                                            Chấm lại
                                        </Button>
                                    </span>
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
