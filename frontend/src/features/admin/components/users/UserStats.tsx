import type { AdminUserDetail } from "@/features/admin/users/types";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

const UNRATED_LABEL = "-";
const STAT_CLASS = "flex flex-col-reverse rounded-md border bg-surface p-2";
const LABEL_CLASS = "text-[11px] text-muted-foreground";
const VALUE_CLASS = "font-mono text-base font-extrabold";

interface UserStatsProps {
    user: AdminUserDetail;
}

export function UserStats({ user }: UserStatsProps) {
    const hasViolations = user.violations.length > 0;

    return (
        <dl className="grid grid-cols-3 gap-2 text-center">
            <div className={STAT_CLASS}>
                <dt className={LABEL_CLASS}>Rating</dt>
                <dd className={VALUE_CLASS}>
                    {user.rating > 0 ? formatNumber(user.rating) : UNRATED_LABEL}
                </dd>
            </div>
            <div className={STAT_CLASS}>
                <dt className={LABEL_CLASS}>Bài AC</dt>
                <dd className={VALUE_CLASS}>{formatNumber(user.acceptedCount)}</dd>
            </div>
            <div
                className={cn(
                    STAT_CLASS,
                    hasViolations ? "border-violation-severe/30 bg-violation-severe/8" : null,
                )}
            >
                <dt className={LABEL_CLASS}>Vi phạm</dt>
                <dd className={cn(VALUE_CLASS, hasViolations ? "text-violation-severe" : null)}>
                    {formatNumber(user.violations.length)}
                </dd>
            </div>
        </dl>
    );
}
