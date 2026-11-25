import { CalendarClock } from "lucide-react";
import Link from "next/link";

import { Skeleton } from "@/components/shared/Skeleton";
import { formatNumber } from "@/lib/format";
import { ROUTES } from "@/lib/routes";

import type { UpcomingContest } from "../../types";
import { DashboardSection } from "./DashboardSection";

interface UpcomingContestsSectionProps {
    contests: UpcomingContest[] | undefined;
}

const startFormatter = new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    weekday: "short",
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
});

export function UpcomingContestsSection({ contests }: UpcomingContestsSectionProps) {
    return (
        <DashboardSection
            title="Contest sắp tới"
            action={{ label: "Tất cả contest", href: ROUTES.contests }}
            className="flex-[1_1_320px]"
        >
            {contests === undefined ? <Skeleton className="h-40 rounded-lg" /> : null}
            {contests?.length === 0 ? (
                <p className="rounded-lg bg-surface-2 p-5 text-muted-foreground">
                    Chưa có contest nào sắp diễn ra.
                </p>
            ) : null}
            {contests && contests.length > 0 ? (
                <ul className="flex flex-col divide-y">
                    {contests.map((contest) => (
                        <li key={contest.id}>
                            <Link
                                href={ROUTES.contest(contest.slug)}
                                className="flex min-h-14 items-center gap-3 py-2.5 hover:text-brand"
                            >
                                <CalendarClock
                                    aria-hidden="true"
                                    className="size-5 shrink-0 text-muted-foreground"
                                />
                                <span className="flex min-w-0 flex-1 flex-col">
                                    <span className="truncate font-semibold">{contest.title}</span>
                                    <span className="text-sm text-muted-foreground">
                                        {`${startFormatter.format(new Date(contest.startAt))}, ${formatNumber(contest.durationMinutes)} phút`}
                                    </span>
                                </span>
                            </Link>
                        </li>
                    ))}
                </ul>
            ) : null}
        </DashboardSection>
    );
}
