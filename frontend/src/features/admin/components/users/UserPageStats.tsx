import type { AdminUserDetail } from "@/features/admin/users/types";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/utils";

const UNRATED_LABEL = "-";

interface UserPageStatsProps {
    user: AdminUserDetail;
}

interface StatCardProps {
    label: string;
    value: string;
    isAlert?: boolean;
}

function StatCard({ label, value, isAlert = false }: StatCardProps) {
    return (
        <div
            className={cn(
                "flex flex-col-reverse rounded-lg border bg-card px-3.5 py-3",
                isAlert ? "border-violation-severe/30 bg-violation-severe/8" : null,
            )}
        >
            <dt className="text-xs text-muted-foreground">{label}</dt>
            <dd
                className={cn(
                    "font-mono text-xl font-extrabold",
                    isAlert ? "text-violation-severe" : null,
                )}
            >
                {value}
            </dd>
        </div>
    );
}

export function UserPageStats({ user }: UserPageStatsProps) {
    const violationCount = user.violations.length;

    return (
        <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
            <StatCard
                label="Rating"
                value={user.rating > 0 ? formatNumber(user.rating) : UNRATED_LABEL}
            />
            <StatCard label="Bài AC" value={formatNumber(user.acceptedCount)} />
            <StatCard label="Bài nộp" value={formatNumber(user.submissionCount)} />
            <StatCard label="Contest" value={formatNumber(user.contestCount)} />
            <StatCard
                label="Vi phạm"
                value={formatNumber(violationCount)}
                isAlert={violationCount > 0}
            />
        </dl>
    );
}
