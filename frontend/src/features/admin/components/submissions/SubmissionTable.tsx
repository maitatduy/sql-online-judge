import Link from "next/link";

import { Button } from "@/components/ui/button";
import { DIALECT_LABELS } from "@/lib/dialect";
import { formatNumber, formatRelativeTime } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { useNow } from "@/lib/useNow";

import { problemLabel } from "../../submissions/labels";
import type { AdminSubmission } from "../../submissions/types";
import { BODY_CELL_CLASS, HEADER_CELL_CLASS } from "../users/table-cells";
import { SubmissionResult } from "./SubmissionResult";

const COLUMNS = ["Lúc nộp", "Người nộp", "Bài", "Dialect", "Nguồn", "Kết quả", "Thời gian chạy"];

interface SubmissionTableProps {
    submissions: AdminSubmission[];
    onOpen: (submissionId: string) => void;
}

export function SubmissionTable({ submissions, onOpen }: SubmissionTableProps) {
    const now = useNow();

    return (
        <div role="region" aria-label="Bảng bài nộp" tabIndex={0} className="overflow-x-auto">
            <table aria-label="Bài nộp" className="w-full min-w-[920px] border-collapse text-sm">
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
                    {submissions.map((submission) => (
                        <tr key={submission.id} className="hover:bg-surface">
                            <td className={`${BODY_CELL_CLASS} whitespace-nowrap`}>
                                <time
                                    dateTime={submission.createdAt}
                                    title={new Date(submission.createdAt).toLocaleString("vi-VN")}
                                >
                                    {formatRelativeTime(submission.createdAt, now)}
                                </time>
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
                                    <span className="text-muted-foreground">Tài khoản đã xoá</span>
                                )}
                            </td>
                            <td className={`${BODY_CELL_CLASS} max-w-72 truncate`}>
                                {problemLabel(submission)}
                            </td>
                            <td className={BODY_CELL_CLASS}>
                                {DIALECT_LABELS[submission.dialect]}
                            </td>
                            <td className={BODY_CELL_CLASS}>
                                {submission.contestId ? "Contest" : "Luyện tập"}
                            </td>
                            <td className={BODY_CELL_CLASS}>
                                <SubmissionResult submission={submission} />
                            </td>
                            <td className={`${BODY_CELL_CLASS} whitespace-nowrap`}>
                                {submission.runtimeMs === null
                                    ? "-"
                                    : `${formatNumber(submission.runtimeMs)} ms`}
                            </td>
                            <td className={`${BODY_CELL_CLASS} text-right`}>
                                <Button
                                    type="button"
                                    size="sm"
                                    variant="outline"
                                    aria-label={`Xem bài nộp của ${submission.username ?? "tài khoản đã xoá"} cho ${problemLabel(submission)}`}
                                    onClick={() => onOpen(submission.id)}
                                >
                                    Xem
                                </Button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
