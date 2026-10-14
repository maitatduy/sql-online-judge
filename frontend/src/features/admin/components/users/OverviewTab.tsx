import Link from "next/link";
import { useState } from "react";

import type { AdminUserDetail } from "@/features/admin/users/types";
import { ROUTES } from "@/lib/routes";

import { NotesCard } from "./NotesCard";
import { PageCard } from "./PageCard";
import { RecentContestsCard } from "./RecentContestsCard";
import { RestrictionsCard } from "./RestrictionsCard";
import { SessionsCard } from "./SessionsCard";
import { UserFacts } from "./UserFacts";
import { ViolationList } from "./ViolationList";

const RECENT_COUNT = 3;

interface OverviewTabProps {
    user: AdminUserDetail;
    onShowContests: () => void;
    onShowSessions: () => void;
}

export function OverviewTab({ user, onShowContests, onShowSessions }: OverviewTabProps) {
    const [mountedAt] = useState(() => new Date());
    const recentViolations = user.violations.slice(-RECENT_COUNT).reverse();

    return (
        <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
            <div className="flex min-w-0 flex-col gap-4">
                <PageCard
                    title="Vi phạm gần đây"
                    action={
                        <Link
                            href={ROUTES.adminReports}
                            className="text-[13px] font-semibold text-brand hover:underline"
                        >
                            Mở hàng đợi báo cáo
                        </Link>
                    }
                >
                    <ViolationList violations={recentViolations} />
                </PageCard>
                <RecentContestsCard username={user.username} onShowAll={onShowContests} />
            </div>
            <div className="flex min-w-0 flex-col gap-4">
                <RestrictionsCard user={user} />
                <SessionsCard
                    username={user.username}
                    isReadOnly={user.deletedAt !== null}
                    limit={RECENT_COUNT}
                    onShowAll={onShowSessions}
                />
                <NotesCard username={user.username} isReadOnly={user.deletedAt !== null} />
                <PageCard title="Thông tin tài khoản">
                    <UserFacts user={user} now={mountedAt} />
                </PageCard>
            </div>
        </div>
    );
}
