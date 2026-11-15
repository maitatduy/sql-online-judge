import Link from "next/link";

import { Button } from "@/components/ui/button";
import { formatNumber } from "@/lib/format";

interface DashboardHeaderProps {
    displayName: string | undefined;
    streakDays: number | undefined;
    challengeHref: string;
    now: Date;
}

const hourFormatter = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Ho_Chi_Minh",
    hour: "numeric",
    hourCycle: "h23",
});

const MORNING_ENDS = 11;
const AFTERNOON_ENDS = 18;

function partOfDay(now: Date): string {
    const hour = Number(hourFormatter.format(now));
    if (hour < MORNING_ENDS) {
        return "sáng";
    }
    return hour < AFTERNOON_ENDS ? "chiều" : "tối";
}

export function DashboardHeader({
    displayName,
    streakDays,
    challengeHref,
    now,
}: DashboardHeaderProps) {
    return (
        <div className="flex flex-wrap items-end justify-between gap-4">
            <div className="flex flex-col gap-1">
                <h1 className="text-[28px] font-extrabold tracking-tight">
                    {`Chào buổi ${partOfDay(now)}${displayName ? `, ${displayName}` : ""}`}
                </h1>
                {streakDays === undefined ? null : (
                    <p className="text-muted-foreground">
                        {streakDays > 0 ? (
                            <>
                                Bạn đang có chuỗi{" "}
                                <strong className="text-streak">
                                    {`${formatNumber(streakDays)} ngày`}
                                </strong>{" "}
                                học liên tục.
                            </>
                        ) : (
                            "Học một bài hôm nay để bắt đầu chuỗi ngày."
                        )}
                    </p>
                )}
            </div>
            <Button asChild size="lg">
                <Link href={challengeHref}>Làm thử thách hôm nay</Link>
            </Button>
        </div>
    );
}
