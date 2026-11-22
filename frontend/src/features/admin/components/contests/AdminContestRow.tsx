import Link from "next/link";

import { ADMIN_ROUTES } from "@/lib/admin-navigation";

import { CONTEST_PHASE_LABELS, formatContestMoment, formatMinutes } from "../../contests/format";
import type { AdminContestSummary } from "../../contests/types";

const CELL_CLASS = "border-b px-4 py-3";

interface AdminContestRowProps {
    contest: AdminContestSummary;
}

export function AdminContestRow({ contest }: AdminContestRowProps) {
    return (
        <tr>
            <td className={CELL_CLASS}>
                <Link
                    href={ADMIN_ROUTES.contest(contest.id)}
                    className="font-semibold text-brand hover:underline"
                >
                    {contest.title}
                </Link>
                <p className="text-sm text-muted-foreground">{contest.slug}</p>
            </td>
            <td className={CELL_CLASS}>{CONTEST_PHASE_LABELS[contest.phase]}</td>
            <td className={CELL_CLASS}>{formatContestMoment(contest.startAt)}</td>
            <td className={CELL_CLASS}>{formatMinutes(contest.durationMinutes)}</td>
            <td className={CELL_CLASS}>{contest.isRated ? "Có" : "Không"}</td>
        </tr>
    );
}
