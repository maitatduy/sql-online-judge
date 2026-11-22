import Link from "next/link";

import { formatMinutes } from "@/lib/contest";
import { formatDate } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import type { ContestSummary } from "../types";

const HEADER_CLASS = "border-b px-4 py-2.5 text-left text-sm font-semibold text-muted-foreground";
const CELL_CLASS = "border-b px-4 py-3";

interface FinishedContestTableProps {
    contests: ContestSummary[];
}

export function FinishedContestTable({ contests }: FinishedContestTableProps) {
    return (
        <div className="overflow-x-auto rounded-lg border bg-card">
            <table
                aria-label="Contest đã kết thúc"
                className="w-full min-w-[480px] border-collapse"
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
                    </tr>
                </thead>
                <tbody>
                    {contests.map((contest) => (
                        <tr key={contest.id}>
                            <td className={CELL_CLASS}>
                                <Link
                                    href={ROUTES.contest(contest.slug)}
                                    className="font-semibold hover:text-brand"
                                >
                                    {contest.title}
                                </Link>
                            </td>
                            <td className={`${CELL_CLASS} text-muted-foreground`}>
                                {formatDate(contest.startAt)}
                            </td>
                            <td className={`${CELL_CLASS} text-muted-foreground`}>
                                {formatMinutes(contest.durationMinutes)}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}
