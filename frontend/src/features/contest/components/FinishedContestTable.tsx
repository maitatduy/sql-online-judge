import { CircleCheck } from "lucide-react";
import Link from "next/link";

import { formatMinutes } from "@/lib/contest";
import { formatDate, formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";
import { cn } from "@/lib/utils";

import type { ContestSummary } from "../types";

const HEADER_CLASS = "border-b px-4 py-2.5 text-left text-sm font-semibold text-muted-foreground";
const CELL_CLASS = "border-b px-4 py-3";
const NUMBER_HEADER_CLASS = `${HEADER_CLASS} text-right`;
const NUMBER_CELL_CLASS = `${CELL_CLASS} text-right font-semibold tabular-nums`;

interface FinishedContestTableProps {
    contests: ContestSummary[];
}

export function FinishedContestTable({ contests }: FinishedContestTableProps) {
    return (
        <div className="overflow-x-auto rounded-lg border bg-card">
            <table
                aria-label="Contest đã kết thúc"
                className="w-full min-w-[640px] border-collapse"
            >
                <thead>
                    <tr>
                        <th scope="col" className={HEADER_CLASS}>
                            Contest
                        </th>
                        <th scope="col" className={HEADER_CLASS}>
                            Ngày
                        </th>
                        <th scope="col" className={HEADER_CLASS}>
                            Thời lượng
                        </th>
                        <th scope="col" className={NUMBER_HEADER_CLASS}>
                            Số bài
                        </th>
                        <th scope="col" className={NUMBER_HEADER_CLASS}>
                            Người tham gia
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {contests.map((contest) => (
                        <tr
                            key={contest.id}
                            className={cn(contest.isRegistered && "bg-brand-soft/40")}
                        >
                            <td
                                className={cn(
                                    CELL_CLASS,
                                    "border-l-[3px] border-l-transparent",
                                    contest.isRegistered && "border-l-brand",
                                )}
                            >
                                <span className="flex flex-wrap items-center gap-2">
                                    <Link
                                        href={ROUTES.contest(contest.slug)}
                                        className="font-semibold hover:text-brand"
                                    >
                                        {contest.title}
                                    </Link>
                                    {contest.isRegistered ? (
                                        <span className="inline-flex items-center gap-1 rounded-full bg-brand/12 px-2 py-0.5 text-[12px] font-semibold text-brand">
                                            <CircleCheck aria-hidden="true" className="size-3.5" />
                                            Đã tham gia
                                        </span>
                                    ) : null}
                                </span>
                            </td>
                            <td className={`${CELL_CLASS} text-muted-foreground`}>
                                {formatDate(contest.startAt)}
                            </td>
                            <td className={`${CELL_CLASS} text-muted-foreground`}>
                                {formatMinutes(contest.durationMinutes)}
                            </td>
                            <td className={NUMBER_CELL_CLASS}>
                                {formatNumber(contest.problemCount)}
                            </td>
                            <td className={NUMBER_CELL_CLASS}>
                                {formatNumber(contest.participantCount)}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
