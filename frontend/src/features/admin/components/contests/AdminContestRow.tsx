import Link from "next/link";

import { Button } from "@/components/ui/button";
import { ADMIN_ROUTES } from "@/lib/admin-navigation";
import { formatContestMoment, formatMinutes } from "@/lib/contest";

import type { AdminContestSummary } from "../../contests/types";
import { AdminContestPhase } from "./AdminContestPhase";

const CELL_CLASS = "border-b px-4 py-3";

interface AdminContestRowProps {
    contest: AdminContestSummary;
}

export function AdminContestRow({ contest }: AdminContestRowProps) {
    const action = contest.phase === "UPCOMING" ? "Sửa" : "Xem";

    return (
        <tr>
            <td className={CELL_CLASS}>
                <Link
                    href={ADMIN_ROUTES.contest(contest.id)}
                    className="font-semibold hover:text-brand"
                >
                    {contest.title}
                </Link>
            </td>
            <td className={`${CELL_CLASS} text-muted-foreground`}>
                {`${formatContestMoment(contest.startAt)}, ${formatMinutes(contest.durationMinutes)}`}
            </td>
            <td className={CELL_CLASS}>{contest.isRated ? "Có" : "Không"}</td>
            <td className={CELL_CLASS}>
                <AdminContestPhase phase={contest.phase} />
            </td>
            <td className={`${CELL_CLASS} text-right`}>
                <Button asChild variant="outline" size="sm">
                    <Link
                        href={ADMIN_ROUTES.contest(contest.id)}
                        aria-label={`${action} ${contest.title}`}
                    >
                        {action}
                    </Link>
                </Button>
            </td>
        </tr>
    );
}
